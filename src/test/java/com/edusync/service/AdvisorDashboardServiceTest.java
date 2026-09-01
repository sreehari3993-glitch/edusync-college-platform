package com.edusync.service;

import com.edusync.dto.AdvisorDto;
import com.edusync.model.Attendance;
import com.edusync.model.InternalMark;
import com.edusync.model.User;
import com.edusync.repository.AttendanceRepository;
import com.edusync.repository.InternalMarkRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdvisorDashboardServiceTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private AttendanceRepository attendanceRepo;

    @Mock
    private InternalMarkRepository internalMarkRepo;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AdvisorDashboardService advisorDashboardService;

    private User facultyAdvisor;
    private User atRiskStudent;
    private User safeStudent;

    @BeforeEach
    void setUp() {
        facultyAdvisor = User.builder()
                .id(10L)
                .name("Prof. Advisor")
                .email("advisor@tkmit.ac.in")
                .role(User.Role.FACULTY)
                .department("CSE")
                .active(true)
                .build();

        atRiskStudent = User.builder()
                .id(101L)
                .name("Arun Kumar")
                .email("arun@tkmit.ac.in")
                .role(User.Role.STUDENT)
                .department("CSE")
                .semester("S6")
                .rollNumber("TKM21CS001")
                .advisor(facultyAdvisor)
                .active(true)
                .build();

        safeStudent = User.builder()
                .id(102L)
                .name("Divya S")
                .email("divya@tkmit.ac.in")
                .role(User.Role.STUDENT)
                .department("CSE")
                .semester("S6")
                .rollNumber("TKM21CS002")
                .advisor(facultyAdvisor)
                .active(true)
                .build();
    }

    @Test
    void testAttendanceCriticalRiskDetectionBelow75Percent() {
        // Given 10 classes, only 6 attended (60% attendance < 75% KTU limit)
        List<Attendance> lowAttendance = List.of(
                Attendance.builder().status(Attendance.AttendanceStatus.PRESENT).build(),
                Attendance.builder().status(Attendance.AttendanceStatus.PRESENT).build(),
                Attendance.builder().status(Attendance.AttendanceStatus.PRESENT).build(),
                Attendance.builder().status(Attendance.AttendanceStatus.PRESENT).build(),
                Attendance.builder().status(Attendance.AttendanceStatus.PRESENT).build(),
                Attendance.builder().status(Attendance.AttendanceStatus.PRESENT).build(),
                Attendance.builder().status(Attendance.AttendanceStatus.ABSENT).build(),
                Attendance.builder().status(Attendance.AttendanceStatus.ABSENT).build(),
                Attendance.builder().status(Attendance.AttendanceStatus.ABSENT).build(),
                Attendance.builder().status(Attendance.AttendanceStatus.ABSENT).build()
        );

        when(attendanceRepo.findByStudent(atRiskStudent)).thenReturn(lowAttendance);
        when(internalMarkRepo.findByStudentOrderByCreatedAtDesc(atRiskStudent)).thenReturn(List.of());

        AdvisorDto.StudentMenteeRiskDto risk = advisorDashboardService.calculateStudentRisk(atRiskStudent);

        assertEquals(60.0, risk.getAttendancePercentage());
        assertEquals(AdvisorDto.RiskLevel.CRITICAL, risk.getAttendanceRisk());
        assertEquals(AdvisorDto.RiskLevel.CRITICAL, risk.getOverallRisk());
        assertTrue(risk.getRiskFactors().stream().anyMatch(rf -> rf.contains("75%")));
    }

    @Test
    void testSeriesMarksCriticalRiskDetectionBelow40Percent() {
        // Given 100% attendance but series mark 15/50 (30% < 40% KTU pass limit)
        List<Attendance> goodAttendance = List.of(
                Attendance.builder().status(Attendance.AttendanceStatus.PRESENT).build(),
                Attendance.builder().status(Attendance.AttendanceStatus.PRESENT).build()
        );

        List<InternalMark> lowMarks = List.of(
                InternalMark.builder().totalMarks(15.0).maxMarks(50.0).build()
        );

        when(attendanceRepo.findByStudent(atRiskStudent)).thenReturn(goodAttendance);
        when(internalMarkRepo.findByStudentOrderByCreatedAtDesc(atRiskStudent)).thenReturn(lowMarks);

        AdvisorDto.StudentMenteeRiskDto risk = advisorDashboardService.calculateStudentRisk(atRiskStudent);

        assertEquals(100.0, risk.getAttendancePercentage());
        assertEquals(AdvisorDto.RiskLevel.SAFE, risk.getAttendanceRisk());
        assertEquals(30.0, risk.getSeriesExamAvgPercentage());
        assertEquals(AdvisorDto.RiskLevel.CRITICAL, risk.getSeriesMarksRisk());
        assertEquals(AdvisorDto.RiskLevel.CRITICAL, risk.getOverallRisk());
        assertTrue(risk.getRiskFactors().stream().anyMatch(rf -> rf.contains("40%")));
    }

    @Test
    void testAdvisorDashboardOverviewAggregations() {
        when(userRepo.findById(10L)).thenReturn(Optional.of(facultyAdvisor));
        when(userRepo.findByAdvisorAndActiveTrue(facultyAdvisor)).thenReturn(List.of(atRiskStudent, safeStudent));

        // Arun has low attendance
        when(attendanceRepo.findByStudent(atRiskStudent)).thenReturn(List.of(
                Attendance.builder().status(Attendance.AttendanceStatus.ABSENT).build()
        ));
        when(internalMarkRepo.findByStudentOrderByCreatedAtDesc(atRiskStudent)).thenReturn(List.of());

        // Divya has good standing
        when(attendanceRepo.findByStudent(safeStudent)).thenReturn(List.of(
                Attendance.builder().status(Attendance.AttendanceStatus.PRESENT).build()
        ));
        when(internalMarkRepo.findByStudentOrderByCreatedAtDesc(safeStudent)).thenReturn(List.of(
                InternalMark.builder().totalMarks(45.0).maxMarks(50.0).build()
        ));

        AdvisorDto.AdvisorBatchOverviewDto dashboard = advisorDashboardService.getAdvisorDashboard(10L);

        assertNotNull(dashboard);
        assertEquals(2, dashboard.getTotalMentees());
        assertEquals(1, dashboard.getCriticalCount());
        assertEquals(1, dashboard.getSafeCount());
        assertEquals("Prof. Advisor", dashboard.getAdvisorName());
    }

    @Test
    void testSendMenteeAlertDispatchesPushAndAuditLog() {
        when(userRepo.findById(10L)).thenReturn(Optional.of(facultyAdvisor));
        when(userRepo.findById(101L)).thenReturn(Optional.of(atRiskStudent));

        AdvisorDto.MenteeAlertRequest req = new AdvisorDto.MenteeAlertRequest(
                101L,
                "IN_APP",
                "STUDENT",
                "Attendance alert: please report to department office"
        );

        assertDoesNotThrow(() -> advisorDashboardService.sendMenteeAlert(10L, req));

        verify(notificationService, times(1)).sendToUser(eq(101L), any());
        verify(auditService, times(1)).log(eq("ADVISORY_ALERT_DISPATCHED"), any(), any(), any(), any(), any(), any());
    }
}
