package com.edusync.service;

import com.edusync.model.Subject;
import com.edusync.model.User;
import com.edusync.repository.AttendanceRepository;
import com.edusync.repository.InternalMarkRepository;
import com.edusync.repository.SubjectRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicCalculatorServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private InternalMarkRepository internalMarkRepository;

    @InjectMocks
    private AcademicCalculatorService calculatorService;

    private User student;
    private Subject subject;

    @BeforeEach
    void setUp() {
        student = User.builder()
                .id(1L)
                .name("Arun Kumar")
                .email("arun@student.tkmit.ac.in")
                .role(User.Role.STUDENT)
                .department("CSE")
                .semester("S4")
                .build();

        subject = Subject.builder()
                .id(101L)
                .name("Discrete Mathematics")
                .code("CST201")
                .department("CSE")
                .semester("S4")
                .build();
    }

    @Test
    void testPredictEseRequired_StandardGradeA() {
        // Internal CIE = 40/50. Target = A (80%).
        // Required ESE = 2 * (80 - 40) = 80/100
        var result = calculatorService.predictEseRequired("Discrete Mathematics", 40.0, "A");

        assertNotNull(result);
        assertEquals(40.0, result.getInternalMarks());
        assertEquals("A", result.getTargetGrade());
        assertEquals(80.0, result.getRequiredEseMarks());
        assertTrue(result.isAchievable());
        assertEquals(90.0, result.getMaxPossibleTotal());
        assertEquals("S", result.getMaxPossibleGrade());
    }

    @Test
    void testPredictEseRequired_HighInternalMinimumPassingFloorRule() {
        // Internal CIE = 48/50. Target = B (70%).
        // Mathematical required = 2 * (70 - 48) = 44, but KTU min pass floor is 40.
        var result = calculatorService.predictEseRequired("Data Structures", 48.0, "B");

        assertNotNull(result);
        assertEquals(44.0, result.getRequiredEseMarks());
        assertTrue(result.isAchievable());
    }

    @Test
    void testPredictEseRequired_UnachievableTarget() {
        // Internal CIE = 25/50. Target = S (90%).
        // 2 * (90 - 25) = 130 > 100
        var result = calculatorService.predictEseRequired("Logic System Design", 25.0, "S");

        assertNotNull(result);
        assertFalse(result.isAchievable());
        assertEquals(75.0, result.getMaxPossibleTotal());
        assertEquals("B+", result.getMaxPossibleGrade());
    }

    @Test
    void testSimulateAttendance_SkipOnePeriodSafe() {
        // Current: 26/30 (86.7%). Miss 1 period -> 26/31 (83.9%)
        var result = calculatorService.simulateAttendance("Discrete Mathematics", 26, 30, 1, 0);

        assertNotNull(result);
        assertEquals(86.7, result.getCurrentPercentage());
        assertEquals(83.9, result.getSimulatedPercentage());
        assertEquals(-2.8, result.getPercentageChange());
        assertEquals("SAFE", result.getStatus());
        assertEquals(4, result.getSafeBunksRemaining());
    }

    @Test
    void testSimulateAttendance_SkipFullDayCondonationRisk() {
        // Current: 24/30 (80.0%). Miss 6 periods -> 24/36 (66.7%) -> Condonation Zone
        var result = calculatorService.simulateAttendance("All Subjects", 24, 30, 6, 0);

        assertNotNull(result);
        assertEquals(66.7, result.getSimulatedPercentage());
        assertEquals("CONDONATION_RISK", result.getStatus());
    }

    @Test
    void testSimulateAttendance_RecoveryCalculation() {
        // Current: 20/30 (66.7%). Needs 10 classes to reach 30/40 (75%)
        var result = calculatorService.simulateAttendance("All Subjects", 20, 30, 0, 0);

        assertNotNull(result);
        assertEquals(10, result.getClassesNeededToRecover75());
    }

    @Test
    void testGetStudentAcademicOverview() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(student));
        when(subjectRepository.findByDepartmentAndSemester("CSE", "S4")).thenReturn(List.of(subject));
        when(attendanceRepository.countPresent(any(), any())).thenReturn(24L);
        when(attendanceRepository.countTotal(any(), any())).thenReturn(28L);
        when(internalMarkRepository.findByStudentOrderByCreatedAtDesc(any())).thenReturn(List.of());

        var overview = calculatorService.getStudentAcademicOverview(1L);

        assertNotNull(overview);
        assertEquals("Arun Kumar", overview.getStudentName());
        assertEquals(1, overview.getSubjects().size());
        assertEquals("Discrete Mathematics", overview.getSubjects().get(0).getSubjectName());
        assertEquals(85.7, overview.getSubjects().get(0).getAttendancePercentage());
    }
}
