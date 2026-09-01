package com.edusync.service;

import com.edusync.dto.NotificationDto;
import com.edusync.model.DeviceToken;
import com.edusync.model.User;
import com.edusync.repository.DeviceTokenRepository;
import com.edusync.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FcmPushNotificationServiceTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FcmPushNotificationService pushService;

    private User studentUser;
    private DeviceToken sampleToken;

    @BeforeEach
    void setUp() {
        studentUser = User.builder()
                .id(10L)
                .name("Rahul Sharma")
                .email("rahul@student.tkmit.ac.in")
                .role(User.Role.STUDENT)
                .department("CSE")
                .semester("S4")
                .build();

        sampleToken = DeviceToken.builder()
                .id(1L)
                .user(studentUser)
                .token("fcm_token_sample_abc123")
                .platform("ANDROID")
                .active(true)
                .build();
    }

    @Test
    void testRegisterNewDeviceToken() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(studentUser));
        when(deviceTokenRepository.findByUserAndToken(studentUser, "fcm_token_sample_abc123")).thenReturn(Optional.empty());
        when(deviceTokenRepository.save(any(DeviceToken.class))).thenReturn(sampleToken);

        DeviceToken result = pushService.registerDeviceToken(10L, "fcm_token_sample_abc123", "ANDROID", "Pixel 8");

        assertNotNull(result);
        assertEquals("fcm_token_sample_abc123", result.getToken());
        assertTrue(result.isActive());
        verify(deviceTokenRepository, times(1)).save(any(DeviceToken.class));
    }

    @Test
    void testRegisterExistingTokenRefreshesActive() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(studentUser));
        when(deviceTokenRepository.findByUserAndToken(studentUser, "fcm_token_sample_abc123")).thenReturn(Optional.of(sampleToken));
        when(deviceTokenRepository.save(any(DeviceToken.class))).thenReturn(sampleToken);

        DeviceToken result = pushService.registerDeviceToken(10L, "fcm_token_sample_abc123", "ANDROID", "Samsung S24");

        assertNotNull(result);
        assertTrue(result.isActive());
        verify(deviceTokenRepository, times(1)).save(sampleToken);
    }

    @Test
    void testUnregisterDeviceToken() {
        assertDoesNotThrow(() -> pushService.unregisterDeviceToken(10L, "fcm_token_sample_abc123"));
        verify(deviceTokenRepository, times(1)).deactivateToken(10L, "fcm_token_sample_abc123");
    }

    @Test
    void testSendPushToUserDoesNotThrow() {
        when(deviceTokenRepository.findByUserIdAndActiveTrue(10L)).thenReturn(List.of(sampleToken));

        NotificationDto dto = NotificationDto.builder()
                .type("ATTENDANCE_SHORTAGE")
                .title("🚨 Attendance Alert")
                .message("Attendance dropped below 75% in CS201")
                .build();

        assertDoesNotThrow(() -> pushService.sendPushToUser(10L, dto));
        verify(deviceTokenRepository, times(1)).findByUserIdAndActiveTrue(10L);
    }

    @Test
    void testBroadcastPushDoesNotThrow() {
        when(deviceTokenRepository.findAllActive()).thenReturn(List.of(sampleToken));

        NotificationDto dto = NotificationDto.builder()
                .type("ANNOUNCEMENT")
                .title("📢 Campus Circular")
                .message("Tomorrow is an official holiday for Onam Celebrations")
                .build();

        assertDoesNotThrow(() -> pushService.broadcastPush(dto));
        verify(deviceTokenRepository, times(1)).findAllActive();
    }
}
