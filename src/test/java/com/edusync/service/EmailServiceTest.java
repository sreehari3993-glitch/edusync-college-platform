package com.edusync.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
        ReflectionTestUtils.setField(emailService, "fromAddress", "notifications@tkmit.ac.in");
        ReflectionTestUtils.setField(emailService, "fromName", "EduSync TKMIT");

        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void testSendParentAcademicAlertEmail() {
        assertDoesNotThrow(() -> emailService.sendParentAcademicAlertEmail(
                "parent@example.com",
                "Arun Kumar",
                "TKM21CS001",
                "CSE",
                68.5,
                34.0,
                "Prof. Advisor",
                "Please attend faculty counseling"
        ));

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendUrgentNoticeEmail() {
        assertDoesNotThrow(() -> emailService.sendUrgentNoticeEmail(
                "student@tkmit.ac.in",
                "Arun Kumar",
                "Campus Holiday Notice",
                "URGENT",
                "Tomorrow is declared a holiday due to heavy rain.",
                "Principal Office"
        ));

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendAssignmentNotificationEmail() {
        assertDoesNotThrow(() -> emailService.sendAssignmentNotificationEmail(
                "student@tkmit.ac.in",
                "Arun Kumar",
                "CST301 Network Programming",
                "Socket Programming Project",
                "Implement a multi-client chat server in Java",
                "2026-09-15",
                20,
                "Prof. Faculty"
        ));

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testSendLeaveStatusEmail() {
        assertDoesNotThrow(() -> emailService.sendLeaveStatusEmail(
                "student@tkmit.ac.in",
                "Arun Kumar",
                "Duty Leave",
                "National Hackathon 2026",
                "2026-09-10",
                "2026-09-12",
                3,
                "APPROVED",
                "Prof. Faculty (Faculty Advisor)",
                "HOD_REVIEW",
                "Recommended for attendance credit"
        ));

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void testEmailServiceGracefullyHandlesNullRecipient() {
        assertDoesNotThrow(() -> emailService.sendParentAcademicAlertEmail(
                null, "Student", "ROLL-1", "CSE", 80.0, 60.0, "Advisor", "Note"
        ));
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
