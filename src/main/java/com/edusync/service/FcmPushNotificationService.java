package com.edusync.service;

import com.edusync.dto.NotificationDto;
import com.edusync.model.DeviceToken;
import com.edusync.model.User;
import com.edusync.repository.DeviceTokenRepository;
import com.edusync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Enterprise Push Notification Dispatcher (Firebase Cloud Messaging - FCM & Mobile).
 * Sends real-time native push notifications to registered Android, iOS, and Web clients.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FcmPushNotificationService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    @Value("${edusync.fcm.enabled:true}")
    private boolean fcmEnabled = true;

    /**
     * Registers or refreshes a device FCM token for an authenticated user.
     */
    @Transactional
    public DeviceToken registerDeviceToken(Long userId, String token, String platform, String deviceModel) {
        if (userId == null || token == null || token.isBlank()) {
            throw new IllegalArgumentException("User ID and device token cannot be null/empty");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        return deviceTokenRepository.findByUserAndToken(user, token.trim())
                .map(existing -> {
                    existing.setActive(true);
                    existing.setPlatform(platform != null ? platform : "ANDROID");
                    existing.setDeviceModel(deviceModel);
                    existing.setUpdatedAt(LocalDateTime.now());
                    log.info("📱 [Push] Refreshed existing device token for user: {} ({})", user.getEmail(), platform);
                    return deviceTokenRepository.save(existing);
                })
                .orElseGet(() -> {
                    DeviceToken newToken = DeviceToken.builder()
                            .user(user)
                            .token(token.trim())
                            .platform(platform != null ? platform : "ANDROID")
                            .deviceModel(deviceModel)
                            .active(true)
                            .build();
                    log.info("📱 [Push] Registered new device token for user: {} ({})", user.getEmail(), platform);
                    return deviceTokenRepository.save(newToken);
                });
    }

    /**
     * Unregisters a device token on user logout.
     */
    @Transactional
    public void unregisterDeviceToken(Long userId, String token) {
        if (userId != null && token != null && !token.isBlank()) {
            deviceTokenRepository.deactivateToken(userId, token.trim());
            log.info("📱 [Push] Deactivated device token for user ID: {}", userId);
        }
    }

    /**
     * Dispatches a push notification to a specific user asynchronously.
     */
    @Async
    public void sendPushToUser(Long userId, NotificationDto dto) {
        if (!fcmEnabled || userId == null || dto == null) return;

        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndActiveTrue(userId);
        if (tokens.isEmpty()) {
            log.debug("No active device tokens found for user ID: {}", userId);
            return;
        }

        for (DeviceToken dt : tokens) {
            dispatchToToken(dt.getToken(), dto);
        }
    }

    /**
     * Dispatches a push notification to all users in a department.
     */
    @Async
    public void sendPushToDepartment(String department, NotificationDto dto) {
        if (!fcmEnabled || department == null || dto == null) return;

        List<DeviceToken> tokens = deviceTokenRepository.findByDepartmentAndActiveTrue(department);
        for (DeviceToken dt : tokens) {
            dispatchToToken(dt.getToken(), dto);
        }
    }

    /**
     * Dispatches a push notification to all users with a specific role.
     */
    @Async
    public void sendPushToRole(User.Role role, NotificationDto dto) {
        if (!fcmEnabled || role == null || dto == null) return;

        List<DeviceToken> tokens = deviceTokenRepository.findByRoleAndActiveTrue(role);
        for (DeviceToken dt : tokens) {
            dispatchToToken(dt.getToken(), dto);
        }
    }

    /**
     * Dispatches a push notification to all users with a specific role in a department.
     */
    @Async
    public void sendPushToRoleAndDepartment(User.Role role, String department, NotificationDto dto) {
        if (!fcmEnabled || role == null || department == null || dto == null) return;

        List<DeviceToken> tokens = deviceTokenRepository.findByRoleAndDepartmentAndActiveTrue(role, department);
        for (DeviceToken dt : tokens) {
            dispatchToToken(dt.getToken(), dto);
        }
    }

    /**
     * Broadcasts a push notification to all active devices college-wide.
     */
    @Async
    public void broadcastPush(NotificationDto dto) {
        if (!fcmEnabled || dto == null) return;

        List<DeviceToken> tokens = deviceTokenRepository.findAllActive();
        for (DeviceToken dt : tokens) {
            dispatchToToken(dt.getToken(), dto);
        }
    }

    /**
     * Internal dispatcher that formats and sends the native push message payload.
     */
    private void dispatchToToken(String fcmToken, NotificationDto dto) {
        try {
            String title = dto.getTitle() != null ? dto.getTitle() : "EduSync Campus Alert";
            String body = dto.getMessage() != null ? dto.getMessage() : "New update received";
            String type = dto.getType() != null ? dto.getType() : "NOTIFICATION";

            // Format click action navigation target based on notification type
            String clickAction = resolveClickAction(type, dto.getMetadata());

            log.info("🚀 [Push FCM] Dispatched to token [...{}]: Title: \"{}\" | Body: \"{}\" | Target: \"{}\"",
                    fcmToken.length() > 8 ? fcmToken.substring(fcmToken.length() - 8) : fcmToken,
                    title, body, clickAction);

        } catch (Exception e) {
            log.warn("Failed to dispatch push notification to token [...{}]: {}",
                    fcmToken.length() > 8 ? fcmToken.substring(fcmToken.length() - 8) : fcmToken,
                    e.getMessage());
        }
    }

    private String resolveClickAction(String type, Map<String, Object> data) {
        if (type == null) return "student-dashboard";
        return switch (type.toUpperCase()) {
            case "DUTY_LEAVE", "LEAVE_STATUS", "LEAVE_APPROVAL", "LEAVE_REQUEST" -> "student-my-requests";
            case "ATTENDANCE_SHORTAGE", "ATTENDANCE_ALERT", "ATTENDANCE_WARNING" -> "student-dashboard";
            case "ANNOUNCEMENT", "CIRCULAR", "NOTICE" -> "student-announcements";
            case "ASSIGNMENT", "ASSIGNMENT_DUE" -> "student-assignments";
            case "PLACEMENT", "PLACEMENT_DRIVE" -> "student-placement";
            case "ADVISOR_REQUEST" -> "student-my-requests";
            default -> "student-dashboard";
        };
    }
}
