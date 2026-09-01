package com.edusync.service;

import com.edusync.dto.NotificationDto;
import com.edusync.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Real-Time Notification Service using Server-Sent Events (SSE).
 * Manages active client emitter connections and broadcasts targeted events.
 */
@Service
@Slf4j
public class NotificationService {

    // Timeout: 30 minutes per emitter
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    // Map: userId -> List of active SseEmitters (supports multiple tabs per user)
    private final Map<Long, List<ClientConnection>> userConnections = new ConcurrentHashMap<>();

    public record ClientConnection(
            Long userId,
            User.Role role,
            String department,
            SseEmitter emitter
    ) {}

    /**
     * Subscribes a client to SSE stream.
     */
    public SseEmitter subscribe(Long userId, User.Role role, String department) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        ClientConnection conn = new ClientConnection(userId, role, department, emitter);

        userConnections.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(conn);

        emitter.onCompletion(() -> removeConnection(userId, conn));
        emitter.onTimeout(() -> {
            emitter.complete();
            removeConnection(userId, conn);
        });
        emitter.onError(e -> {
            removeConnection(userId, conn);
        });

        // Send initial connected handshake event
        try {
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .id(UUID.randomUUID().toString())
                    .data(NotificationDto.builder()
                            .type("CONNECTED")
                            .title("Connected to EduSync Live Stream")
                            .message("Real-time notifications active")
                            .build())
            );
        } catch (IOException e) {
            removeConnection(userId, conn);
        }

        log.debug("SSE client subscribed: user={}, role={}, dept={}", userId, role, department);
        return emitter;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private FcmPushNotificationService fcmPushService;

    public NotificationService() {}

    public NotificationService(FcmPushNotificationService fcmPushService) {
        this.fcmPushService = fcmPushService;
    }

    /**
     * Sends a notification to a specific user.
     */
    public void sendToUser(Long userId, NotificationDto dto) {
        if (dto.getId() == null) {
            dto.setId(UUID.randomUUID().toString());
        }
        List<ClientConnection> connections = userConnections.get(userId);
        if (connections != null && !connections.isEmpty()) {
            for (ClientConnection conn : connections) {
                dispatch(conn, dto);
            }
        }
        // Asynchronously dispatch native mobile push notification (FCM)
        if (fcmPushService != null) {
            fcmPushService.sendPushToUser(userId, dto);
        }
    }

    /**
     * Sends a notification to all users belonging to a specific department.
     */
    public void sendToDepartment(String department, NotificationDto dto) {
        if (department == null) {
            broadcast(dto);
            return;
        }
        if (dto.getId() == null) {
            dto.setId(UUID.randomUUID().toString());
        }
        dto.setDepartment(department);

        userConnections.values().forEach(connList -> {
            for (ClientConnection conn : connList) {
                if (department.equalsIgnoreCase(conn.department())) {
                    dispatch(conn, dto);
                }
            }
        });

        // Asynchronously dispatch native mobile push notification (FCM)
        if (fcmPushService != null) {
            fcmPushService.sendPushToDepartment(department, dto);
        }
    }

    /**
     * Sends a notification to all users matching a specific role (e.g. FACULTY or STUDENT).
     */
    public void sendToRole(User.Role role, NotificationDto dto) {
        if (dto.getId() == null) {
            dto.setId(UUID.randomUUID().toString());
        }
        dto.setTargetRole(role != null ? role.name() : null);

        userConnections.values().forEach(connList -> {
            for (ClientConnection conn : connList) {
                if (role == null || role.equals(conn.role())) {
                    dispatch(conn, dto);
                }
            }
        });

        // Asynchronously dispatch native mobile push notification (FCM)
        if (fcmPushService != null) {
            fcmPushService.sendPushToRole(role, dto);
        }
    }

    /**
     * Sends a notification to all users with a specific role in a specific department.
     */
    public void sendToRoleAndDepartment(User.Role role, String department, NotificationDto dto) {
        if (dto.getId() == null) {
            dto.setId(UUID.randomUUID().toString());
        }
        dto.setDepartment(department);
        dto.setTargetRole(role != null ? role.name() : null);

        userConnections.values().forEach(connList -> {
            for (ClientConnection conn : connList) {
                boolean roleMatch = (role == null || role.equals(conn.role()));
                boolean deptMatch = (department == null || department.equalsIgnoreCase(conn.department()));
                if (roleMatch && deptMatch) {
                    dispatch(conn, dto);
                }
            }
        });

        // Asynchronously dispatch native mobile push notification (FCM)
        if (fcmPushService != null) {
            fcmPushService.sendPushToRoleAndDepartment(role, department, dto);
        }
    }

    /**
     * Broadcasts a notification to ALL connected users college-wide.
     */
    public void broadcast(NotificationDto dto) {
        if (dto.getId() == null) {
            dto.setId(UUID.randomUUID().toString());
        }
        userConnections.values().forEach(connList -> {
            for (ClientConnection conn : connList) {
                dispatch(conn, dto);
            }
        });

        // Asynchronously dispatch native mobile push notification (FCM)
        if (fcmPushService != null) {
            fcmPushService.broadcastPush(dto);
        }
    }

    /**
     * Sends periodic heartbeat ping every 25 seconds to keep SSE tunnels alive through proxies/browsers.
     */
    @Scheduled(fixedRate = 25000)
    public void sendHeartbeat() {
        userConnections.values().forEach(connList -> {
            for (ClientConnection conn : connList) {
                try {
                    conn.emitter().send(SseEmitter.event().comment("ping"));
                } catch (Exception e) {
                    removeConnection(conn.userId(), conn);
                }
            }
        });
    }

    /**
     * Helper to safely dispatch an SSE payload.
     */
    private void dispatch(ClientConnection conn, NotificationDto dto) {
        try {
            conn.emitter().send(SseEmitter.event()
                    .name(dto.getType() != null ? dto.getType() : "NOTIFICATION")
                    .id(dto.getId())
                    .data(dto)
            );
        } catch (IOException | IllegalStateException e) {
            removeConnection(conn.userId(), conn);
        }
    }

    /**
     * Cleans up dead connections.
     */
    private void removeConnection(Long userId, ClientConnection conn) {
        List<ClientConnection> list = userConnections.get(userId);
        if (list != null) {
            list.remove(conn);
            if (list.isEmpty()) {
                userConnections.remove(userId);
            }
        }
    }

    public int getActiveConnectionCount() {
        return userConnections.values().stream().mapToInt(List::size).sum();
    }
}
