package com.edusync.service;

import com.edusync.dto.NotificationDto;
import com.edusync.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.*;

class NotificationServiceTest {

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService();
    }

    @Test
    void testSubscribeAndActiveCount() {
        SseEmitter emitter1 = notificationService.subscribe(101L, User.Role.STUDENT, "CSE");
        assertNotNull(emitter1);
        assertEquals(1, notificationService.getActiveConnectionCount());

        SseEmitter emitter2 = notificationService.subscribe(201L, User.Role.FACULTY, "CSE");
        assertNotNull(emitter2);
        assertEquals(2, notificationService.getActiveConnectionCount());
    }

    @Test
    void testSendToUserDoesNotThrow() {
        notificationService.subscribe(101L, User.Role.STUDENT, "CSE");

        NotificationDto notif = NotificationDto.builder()
                .type("LEAVE_UPDATE")
                .title("Leave Approved")
                .message("Your leave has been approved by Faculty")
                .actorName("Prof. Smith")
                .build();

        assertDoesNotThrow(() -> notificationService.sendToUser(101L, notif));
        assertDoesNotThrow(() -> notificationService.sendToUser(999L, notif)); // non-existent user
    }

    @Test
    void testSendToDepartmentAndRole() {
        notificationService.subscribe(101L, User.Role.STUDENT, "CSE");
        notificationService.subscribe(102L, User.Role.STUDENT, "ECE");
        notificationService.subscribe(201L, User.Role.FACULTY, "CSE");

        NotificationDto deptNotif = NotificationDto.builder()
                .type("NOTICE_PUBLISHED")
                .title("CSE Department Meeting")
                .message("All CSE members report to seminar hall")
                .build();

        assertDoesNotThrow(() -> notificationService.sendToDepartment("CSE", deptNotif));
        assertDoesNotThrow(() -> notificationService.sendToRole(User.Role.STUDENT, deptNotif));
        assertDoesNotThrow(() -> notificationService.sendToRoleAndDepartment(User.Role.STUDENT, "CSE", deptNotif));
        assertDoesNotThrow(() -> notificationService.broadcast(deptNotif));
    }

    @Test
    void testHeartbeatPing() {
        notificationService.subscribe(101L, User.Role.STUDENT, "CSE");
        assertDoesNotThrow(() -> notificationService.sendHeartbeat());
    }
}
