package com.edusync.controller;

import com.edusync.dto.AppDto;
import com.edusync.dto.NotificationDto;
import com.edusync.model.User;
import com.edusync.security.JwtUtil;
import com.edusync.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * Controller for Real-Time SSE Notifications.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtUtil jwtUtil;

    /**
     * SSE Event Stream Subscription Endpoint.
     * Accessible by browser EventSource.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @RequestParam(required = false) String token,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String department,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Long resolvedUserId = userId;
        User.Role resolvedRole = null;
        String resolvedDept = department;

        String jwtToken = token;
        if ((jwtToken == null || jwtToken.isBlank()) && authHeader != null && authHeader.startsWith("Bearer ")) {
            jwtToken = authHeader.substring(7);
        }

        if (jwtToken != null && !jwtToken.isBlank()) {
            try {
                if (resolvedUserId == null) {
                    resolvedUserId = jwtUtil.extractUserId(jwtToken);
                }
                if (role == null) {
                    String roleStr = jwtUtil.extractRole(jwtToken);
                    if (roleStr != null) {
                        try {
                            resolvedRole = User.Role.valueOf(roleStr);
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception e) {
                log.warn("Invalid JWT on notification stream subscription: {}", e.getMessage());
            }
        }

        if (role != null && resolvedRole == null) {
            try {
                resolvedRole = User.Role.valueOf(role);
            } catch (Exception ignored) {}
        }

        if (resolvedUserId == null) {
            // Fallback synthetic guest ID if completely unauthenticated
            resolvedUserId = (long) (Math.random() * -100000);
        }

        return notificationService.subscribe(resolvedUserId, resolvedRole, resolvedDept);
    }

    /**
     * Test trigger to demonstrate live notifications.
     */
    @PostMapping("/test-broadcast")
    public ResponseEntity<AppDto.ApiResponse<Map<String, Object>>> testBroadcast(
            @RequestParam(defaultValue = "Live Demo Test Alert") String title,
            @RequestParam(defaultValue = "This is a real-time instant notification broadcast!") String message
    ) {
        NotificationDto dto = NotificationDto.builder()
                .type("TEST_ALERT")
                .title(title)
                .message(message)
                .actorName("EduSync System")
                .build();

        notificationService.broadcast(dto);

        return ResponseEntity.ok(AppDto.ApiResponse.ok(
                "Broadcast sent to " + notificationService.getActiveConnectionCount() + " active connections",
                Map.of("activeClients", notificationService.getActiveConnectionCount(), "notification", dto)
        ));
    }
}
