package com.edusync.service;

import com.edusync.dto.IndustryDto;
import com.edusync.dto.NotificationDto;
import com.edusync.exception.BadRequestException;
import com.edusync.model.*;
import com.edusync.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndustryServiceTest {

    @Mock
    private CompanyRepository companyRepo;

    @Mock
    private IndustryMouRepository mouRepo;

    @Mock
    private InternshipRepository internshipRepo;

    @Mock
    private InternshipApplicationRepository applicationRepo;

    @Mock
    private IndustryVisitRepository visitRepo;

    @Mock
    private GuestLectureRepository guestLectureRepo;

    @Mock
    private IndustryProjectRepository industryProjectRepo;

    @Mock
    private AdvisoryMemberRepository advisoryMemberRepo;

    @Mock
    private AlumniContactRepository alumniContactRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private IndustryActivityLogRepository activityLogRepo;

    @Mock
    private NotificationService notificationService;

    @Mock
    private GoogleDriveService googleDriveService;

    @Mock
    private PdfService pdfService;

    @InjectMocks
    private IndustryService industryService;

    private Company testCompany;
    private User testStudent;
    private User testFaculty;
    private Internship testInternship;
    private LocalDate referenceDate;

    @BeforeEach
    void setUp() {
        referenceDate = LocalDate.of(2026, 10, 15);

        testCompany = Company.builder()
                .id(1L)
                .name("TechCorp Solutions")
                .sector("IT / Cloud")
                .partnershipStage(Company.PartnershipStage.ACTIVE)
                .placementReady(true)
                .build();

        testStudent = User.builder()
                .id(200L)
                .name("Dev Anand")
                .email("dev.s7cse@tkmit.ac.in")
                .role(User.Role.STUDENT)
                .department("CSE")
                .semester("S7")
                .build();

        testFaculty = User.builder()
                .id(300L)
                .name("Dr. Smitha S")
                .email("smitha.cse@tkmit.ac.in")
                .role(User.Role.FACULTY)
                .department("CSE")
                .build();

        testInternship = Internship.builder()
                .id(50L)
                .company(testCompany)
                .title("Cloud Infrastructure Intern")
                .departments("CSE,ECE")
                .seats(3)
                .deadline(LocalDate.now().plusDays(10))
                .status(Internship.InternshipStatus.OPEN)
                .build();
    }

    // =========================================================================
    // MOU EXPIRATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Should transition ACTIVE MoU to EXPIRING when validTo is within 60 days")
    void testActiveToExpiringTransitionWithin60Days() {
        IndustryMou mou = IndustryMou.builder()
                .id(101L)
                .company(testCompany)
                .title("Cloud Academy MoU")
                .status(IndustryMou.MouStatus.ACTIVE)
                .validFrom(referenceDate.minusMonths(11))
                .validTo(referenceDate.plusDays(30))
                .build();

        when(mouRepo.findAll()).thenReturn(List.of(mou));

        int transitioned = industryService.processDailyMouExpirationsForDate(referenceDate);

        assertEquals(1, transitioned);
        assertEquals(IndustryMou.MouStatus.EXPIRING, mou.getStatus());
        verify(mouRepo, times(1)).save(mou);
        verify(notificationService, atLeastOnce()).sendToRole(eq(User.Role.INDUSTRIAL_CONNECT_HEAD), any(NotificationDto.class));
        verify(activityLogRepo, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("Should transition ACTIVE MoU to EXPIRED when validTo is today or in the past")
    void testActiveToExpiredTransitionPastValidTo() {
        IndustryMou mou = IndustryMou.builder()
                .id(102L)
                .company(testCompany)
                .title("Hardware Lab MoU")
                .status(IndustryMou.MouStatus.ACTIVE)
                .validFrom(referenceDate.minusYears(1))
                .validTo(referenceDate.minusDays(1))
                .build();

        when(mouRepo.findAll()).thenReturn(List.of(mou));

        int transitioned = industryService.processDailyMouExpirationsForDate(referenceDate);

        assertEquals(1, transitioned);
        assertEquals(IndustryMou.MouStatus.EXPIRED, mou.getStatus());
        verify(mouRepo, times(1)).save(mou);
        verify(notificationService, atLeastOnce()).sendToRole(eq(User.Role.INDUSTRIAL_CONNECT_HEAD), any(NotificationDto.class));
    }

    @Test
    @DisplayName("Should transition EXPIRING MoU to EXPIRED when validTo reaches current date")
    void testExpiringToExpiredTransitionOnCurrentDate() {
        IndustryMou mou = IndustryMou.builder()
                .id(103L)
                .company(testCompany)
                .title("Robotics Research MoU")
                .status(IndustryMou.MouStatus.EXPIRING)
                .validFrom(referenceDate.minusYears(1))
                .validTo(referenceDate)
                .build();

        when(mouRepo.findAll()).thenReturn(List.of(mou));

        int transitioned = industryService.processDailyMouExpirationsForDate(referenceDate);

        assertEquals(1, transitioned);
        assertEquals(IndustryMou.MouStatus.EXPIRED, mou.getStatus());
        verify(mouRepo, times(1)).save(mou);
    }

    @Test
    @DisplayName("Should NOT transition ACTIVE MoU if validTo is beyond 60 days")
    void testNoTransitionWhenValidToBeyond60Days() {
        IndustryMou mou = IndustryMou.builder()
                .id(104L)
                .company(testCompany)
                .title("AI CoE MoU")
                .status(IndustryMou.MouStatus.ACTIVE)
                .validFrom(referenceDate)
                .validTo(referenceDate.plusDays(120))
                .build();

        when(mouRepo.findAll()).thenReturn(List.of(mou));

        int transitioned = industryService.processDailyMouExpirationsForDate(referenceDate);

        assertEquals(0, transitioned);
        assertEquals(IndustryMou.MouStatus.ACTIVE, mou.getStatus());
        verify(mouRepo, never()).save(mou);
        verify(notificationService, never()).sendToRole(any(), any());
    }

    @Test
    @DisplayName("Should ignore DRAFT and TERMINATED MoUs regardless of validTo date")
    void testIgnoreDraftAndTerminatedMous() {
        IndustryMou draftMou = IndustryMou.builder()
                .id(105L)
                .company(testCompany)
                .title("Draft IoT MoU")
                .status(IndustryMou.MouStatus.DRAFT)
                .validTo(referenceDate.minusDays(10))
                .build();

        IndustryMou terminatedMou = IndustryMou.builder()
                .id(106L)
                .company(testCompany)
                .title("Terminated MoU")
                .status(IndustryMou.MouStatus.TERMINATED)
                .validTo(referenceDate.minusDays(10))
                .build();

        when(mouRepo.findAll()).thenReturn(List.of(draftMou, terminatedMou));

        int transitioned = industryService.processDailyMouExpirationsForDate(referenceDate);

        assertEquals(0, transitioned);
        assertEquals(IndustryMou.MouStatus.DRAFT, draftMou.getStatus());
        assertEquals(IndustryMou.MouStatus.TERMINATED, terminatedMou.getStatus());
        verify(mouRepo, never()).save(any());
    }

    @Test
    @DisplayName("Should handle null validTo dates gracefully")
    void testHandleNullValidToGracefully() {
        IndustryMou mouWithNullValidTo = IndustryMou.builder()
                .id(107L)
                .company(testCompany)
                .title("Undated MoU")
                .status(IndustryMou.MouStatus.ACTIVE)
                .validTo(null)
                .build();

        when(mouRepo.findAll()).thenReturn(List.of(mouWithNullValidTo));

        assertDoesNotThrow(() -> {
            int transitioned = industryService.processDailyMouExpirationsForDate(referenceDate);
            assertEquals(0, transitioned);
        });

        assertEquals(IndustryMou.MouStatus.ACTIVE, mouWithNullValidTo.getStatus());
        verify(mouRepo, never()).save(any());
    }

    // =========================================================================
    // INTERNSHIP BUSINESS RULE TESTS
    // =========================================================================

    @Test
    @DisplayName("Rule: Only companies with an ACTIVE MoU can have internships posted")
    void testCreateInternship_ThrowsWhenNoActiveMou() {
        when(companyRepo.findById(1L)).thenReturn(Optional.of(testCompany));
        // No active MoUs for this company
        when(mouRepo.findByCompanyId(1L)).thenReturn(List.of());

        IndustryDto.InternshipRequest req = IndustryDto.InternshipRequest.builder()
                .companyId(1L)
                .title("Software Engineering Intern")
                .seats(5)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                industryService.createInternship(req, "ICH_ADMIN"));

        assertTrue(ex.getMessage().contains("ACTIVE MoU"));
        verify(internshipRepo, never()).save(any());
    }

    @Test
    @DisplayName("Rule: Should block internship application when deadline has passed")
    void testApplyForInternship_ThrowsWhenDeadlineHasPassed() {
        testInternship.setDeadline(LocalDate.now().minusDays(1)); // yesterday

        when(internshipRepo.findById(50L)).thenReturn(Optional.of(testInternship));
        when(userRepo.findById(200L)).thenReturn(Optional.of(testStudent));

        IndustryDto.InternshipApplicationRequest req = IndustryDto.InternshipApplicationRequest.builder()
                .resumeUrl("https://drive.google.com/resume.pdf")
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                industryService.applyForInternship(50L, 200L, req, testStudent.getEmail()));

        assertTrue(ex.getMessage().contains("deadline"));
        verify(applicationRepo, never()).save(any());
    }

    @Test
    @DisplayName("Rule: Should block duplicate applications from the same student")
    void testApplyForInternship_ThrowsWhenDuplicateApplication() {
        when(internshipRepo.findById(50L)).thenReturn(Optional.of(testInternship));
        when(userRepo.findById(200L)).thenReturn(Optional.of(testStudent));
        // Student already applied
        when(applicationRepo.existsByInternshipIdAndStudentId(50L, 200L)).thenReturn(true);

        IndustryDto.InternshipApplicationRequest req = IndustryDto.InternshipApplicationRequest.builder()
                .resumeUrl("https://drive.google.com/resume.pdf")
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                industryService.applyForInternship(50L, 200L, req, testStudent.getEmail()));

        assertTrue(ex.getMessage().contains("already applied"));
        verify(applicationRepo, never()).save(any());
    }

    @Test
    @DisplayName("Rule: Should enforce seat limits when applying")
    void testApplyForInternship_ThrowsWhenSeatLimitReached() {
        testInternship.setSeats(3);

        when(internshipRepo.findById(50L)).thenReturn(Optional.of(testInternship));
        when(userRepo.findById(200L)).thenReturn(Optional.of(testStudent));
        when(applicationRepo.existsByInternshipIdAndStudentId(50L, 200L)).thenReturn(false);
        // Already 3 applicants applied (capacity full)
        when(applicationRepo.countByInternshipId(50L)).thenReturn(3L);

        IndustryDto.InternshipApplicationRequest req = IndustryDto.InternshipApplicationRequest.builder()
                .resumeUrl("https://drive.google.com/resume.pdf")
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                industryService.applyForInternship(50L, 200L, req, testStudent.getEmail()));

        assertTrue(ex.getMessage().contains("seat limit reached") || ex.getMessage().contains("no seats remaining"));
        verify(applicationRepo, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully apply when valid and dispatch FCM notification")
    void testApplyForInternship_SuccessAndDispatchesFCM() {
        when(internshipRepo.findById(50L)).thenReturn(Optional.of(testInternship));
        when(userRepo.findById(200L)).thenReturn(Optional.of(testStudent));
        when(applicationRepo.existsByInternshipIdAndStudentId(50L, 200L)).thenReturn(false);
        when(applicationRepo.countByInternshipId(50L)).thenReturn(1L);

        when(applicationRepo.save(any(InternshipApplication.class))).thenAnswer(inv -> {
            InternshipApplication app = inv.getArgument(0);
            app.setId(999L);
            return app;
        });

        IndustryDto.InternshipApplicationRequest req = IndustryDto.InternshipApplicationRequest.builder()
                .resumeUrl("https://drive.google.com/resume.pdf")
                .build();

        IndustryDto.InternshipApplicationDto result = industryService.applyForInternship(50L, 200L, req, testStudent.getEmail());

        assertNotNull(result);
        assertEquals(InternshipApplication.ApplicationStatus.APPLIED, result.getStatus());
        verify(applicationRepo, times(1)).save(any());
        // FCM notification dispatched to student ID
        verify(notificationService, times(1)).sendToUser(eq(200L), any(NotificationDto.class));
    }

    @Test
    @DisplayName("Should update application status and notify student via FCM on status change")
    void testUpdateApplicationStatus_SendsFCMNotification() {
        InternshipApplication app = InternshipApplication.builder()
                .id(888L)
                .internship(testInternship)
                .student(testStudent)
                .status(InternshipApplication.ApplicationStatus.APPLIED)
                .build();

        when(applicationRepo.findById(888L)).thenReturn(Optional.of(app));
        when(applicationRepo.save(any(InternshipApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        IndustryDto.InternshipApplicationStatusRequest statusReq = IndustryDto.InternshipApplicationStatusRequest.builder()
                .status(InternshipApplication.ApplicationStatus.SELECTED)
                .feedback("Congratulations! Selected for Summer 2026 cohort.")
                .build();

        IndustryDto.InternshipApplicationDto result = industryService.updateApplicationStatus(888L, statusReq, "ICH_OFFICER");

        assertNotNull(result);
        assertEquals(InternshipApplication.ApplicationStatus.SELECTED, result.getStatus());
        verify(applicationRepo, times(1)).save(app);
        verify(notificationService, times(1)).sendToUser(eq(200L), any(NotificationDto.class));
    }

    // =========================================================================
    // INDUSTRIAL VISIT APPROVAL FLOW TESTS
    // =========================================================================

    @Test
    @DisplayName("Propose visit creates visit in ICH_REVIEW stage and sends notification to proposer")
    void testProposeVisit_CreatesVisitInIchReviewAndDispatchesNotification() {
        when(companyRepo.findById(1L)).thenReturn(Optional.of(testCompany));
        when(userRepo.findById(300L)).thenReturn(Optional.of(testFaculty));
        when(visitRepo.save(any(IndustryVisit.class))).thenAnswer(inv -> {
            IndustryVisit v = inv.getArgument(0);
            v.setId(701L);
            return v;
        });

        IndustryDto.IndustryVisitRequest req = IndustryDto.IndustryVisitRequest.builder()
                .companyId(1L)
                .department("CSE")
                .date(LocalDate.now().plusDays(20))
                .studentCount(40)
                .purpose("Cloud & DevOps Industrial Exposure")
                .overnight(false)
                .outsideKerala(false)
                .build();

        IndustryDto.IndustryVisitDto result = industryService.proposeVisit(req, 300L, testFaculty.getEmail());

        assertNotNull(result);
        assertEquals(IndustryVisit.VisitStage.ICH_REVIEW, result.getStage());
        assertEquals(IndustryVisit.VisitStatus.PENDING, result.getStatus());
        assertEquals(IndustryVisit.ApprovalStatus.PENDING, result.getIchStatus());
        assertEquals(IndustryVisit.ApprovalStatus.PENDING, result.getHodStatus());
        assertEquals(IndustryVisit.ApprovalStatus.NOT_REQUIRED, result.getPrincipalStatus());
        verify(visitRepo, times(1)).save(any(IndustryVisit.class));
        verify(notificationService, times(1)).sendToUser(eq(300L), any(NotificationDto.class));
        verify(activityLogRepo, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("ICH approve transitions visit to HOD_REVIEW stage")
    void testIchVisitAction_Approve_TransitionsToHodReview() {
        IndustryVisit visit = IndustryVisit.builder()
                .id(702L)
                .company(testCompany)
                .proposedBy(testFaculty)
                .department("CSE")
                .date(LocalDate.now().plusDays(25))
                .studentCount(35)
                .stage(IndustryVisit.VisitStage.ICH_REVIEW)
                .status(IndustryVisit.VisitStatus.PENDING)
                .build();

        when(visitRepo.findById(702L)).thenReturn(Optional.of(visit));
        when(visitRepo.save(any(IndustryVisit.class))).thenAnswer(inv -> inv.getArgument(0));

        IndustryDto.VisitActionRequest req = IndustryDto.VisitActionRequest.builder()
                .status(IndustryVisit.ApprovalStatus.APPROVED)
                .remark("Endorsed by ICH team")
                .build();

        IndustryDto.IndustryVisitDto result = industryService.ichVisitAction(702L, req, "ich_head@tkmit.ac.in");

        assertNotNull(result);
        assertEquals(IndustryVisit.VisitStage.HOD_REVIEW, result.getStage());
        assertEquals(IndustryVisit.ApprovalStatus.APPROVED, result.getIchStatus());
        assertEquals("Endorsed by ICH team", result.getIchRemark());
        verify(notificationService, times(1)).sendToUser(eq(300L), any(NotificationDto.class));
        verify(activityLogRepo, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("ICH reject marks visit COMPLETED and REJECTED")
    void testIchVisitAction_Reject_MarksCompletedAndRejected() {
        IndustryVisit visit = IndustryVisit.builder()
                .id(703L)
                .company(testCompany)
                .proposedBy(testFaculty)
                .department("CSE")
                .date(LocalDate.now().plusDays(25))
                .studentCount(35)
                .stage(IndustryVisit.VisitStage.ICH_REVIEW)
                .status(IndustryVisit.VisitStatus.PENDING)
                .build();

        when(visitRepo.findById(703L)).thenReturn(Optional.of(visit));
        when(visitRepo.save(any(IndustryVisit.class))).thenAnswer(inv -> inv.getArgument(0));

        IndustryDto.VisitActionRequest req = IndustryDto.VisitActionRequest.builder()
                .status(IndustryVisit.ApprovalStatus.REJECTED)
                .remark("Company not accredited for academic visits")
                .build();

        IndustryDto.IndustryVisitDto result = industryService.ichVisitAction(703L, req, "ich_head@tkmit.ac.in");

        assertNotNull(result);
        assertEquals(IndustryVisit.VisitStage.COMPLETED, result.getStage());
        assertEquals(IndustryVisit.VisitStatus.REJECTED, result.getStatus());
        assertEquals(IndustryVisit.ApprovalStatus.REJECTED, result.getIchStatus());
        verify(notificationService, times(1)).sendToUser(eq(300L), any(NotificationDto.class));
    }

    @Test
    @DisplayName("Non-Principal path: HOD approves local day trip <= threshold -> APPROVED, stage COMPLETED")
    void testHodVisitAction_DirectApproval_NonPrincipalPath() {
        industryService.setLargeGroupThreshold(50);

        IndustryVisit visit = IndustryVisit.builder()
                .id(704L)
                .company(testCompany)
                .proposedBy(testFaculty)
                .department("CSE")
                .date(LocalDate.now().plusDays(15))
                .studentCount(40) // <= 50
                .overnight(false)
                .outsideKerala(false)
                .stage(IndustryVisit.VisitStage.HOD_REVIEW)
                .status(IndustryVisit.VisitStatus.PENDING)
                .ichStatus(IndustryVisit.ApprovalStatus.APPROVED)
                .principalStatus(IndustryVisit.ApprovalStatus.NOT_REQUIRED)
                .build();

        when(visitRepo.findById(704L)).thenReturn(Optional.of(visit));
        when(visitRepo.save(any(IndustryVisit.class))).thenAnswer(inv -> inv.getArgument(0));

        IndustryDto.VisitActionRequest req = IndustryDto.VisitActionRequest.builder()
                .status(IndustryVisit.ApprovalStatus.APPROVED)
                .remark("Approved by HOD. No overnight stay required.")
                .build();

        IndustryDto.IndustryVisitDto result = industryService.hodVisitAction(704L, 400L, req, "hod_cse@tkmit.ac.in");

        assertNotNull(result);
        assertEquals(IndustryVisit.VisitStage.COMPLETED, result.getStage());
        assertEquals(IndustryVisit.VisitStatus.APPROVED, result.getStatus());
        assertEquals(IndustryVisit.ApprovalStatus.APPROVED, result.getHodStatus());
        assertEquals(IndustryVisit.ApprovalStatus.NOT_REQUIRED, result.getPrincipalStatus());
        assertNotNull(result.getPdfPath());
        verify(notificationService, times(1)).sendToUser(eq(300L), any(NotificationDto.class));
        verify(activityLogRepo, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("Principal path: HOD approves overnight visit -> escalates to PRINCIPAL_REVIEW")
    void testHodVisitAction_EscalatesToPrincipal_WhenOvernight() {
        industryService.setLargeGroupThreshold(50);

        IndustryVisit visit = IndustryVisit.builder()
                .id(705L)
                .company(testCompany)
                .proposedBy(testFaculty)
                .department("CSE")
                .date(LocalDate.now().plusDays(20))
                .studentCount(30)
                .overnight(true) // Overnight!
                .outsideKerala(false)
                .stage(IndustryVisit.VisitStage.HOD_REVIEW)
                .status(IndustryVisit.VisitStatus.PENDING)
                .ichStatus(IndustryVisit.ApprovalStatus.APPROVED)
                .principalStatus(IndustryVisit.ApprovalStatus.PENDING)
                .build();

        when(visitRepo.findById(705L)).thenReturn(Optional.of(visit));
        when(visitRepo.save(any(IndustryVisit.class))).thenAnswer(inv -> inv.getArgument(0));

        IndustryDto.VisitActionRequest req = IndustryDto.VisitActionRequest.builder()
                .status(IndustryVisit.ApprovalStatus.APPROVED)
                .remark("Endorsed. Overnight stay requires Principal sanction.")
                .build();

        IndustryDto.IndustryVisitDto result = industryService.hodVisitAction(705L, 400L, req, "hod_cse@tkmit.ac.in");

        assertNotNull(result);
        assertEquals(IndustryVisit.VisitStage.PRINCIPAL_REVIEW, result.getStage());
        assertEquals(IndustryVisit.VisitStatus.PENDING, result.getStatus());
        assertEquals(IndustryVisit.ApprovalStatus.PENDING, result.getPrincipalStatus());
        verify(notificationService, times(1)).sendToUser(eq(300L), any(NotificationDto.class));
    }

    @Test
    @DisplayName("Principal path: HOD approves out-of-state visit -> escalates to PRINCIPAL_REVIEW")
    void testHodVisitAction_EscalatesToPrincipal_WhenOutsideKerala() {
        industryService.setLargeGroupThreshold(50);

        IndustryVisit visit = IndustryVisit.builder()
                .id(706L)
                .company(testCompany)
                .proposedBy(testFaculty)
                .department("CSE")
                .date(LocalDate.now().plusDays(20))
                .studentCount(35)
                .overnight(false)
                .outsideKerala(true) // Outside Kerala!
                .stage(IndustryVisit.VisitStage.HOD_REVIEW)
                .status(IndustryVisit.VisitStatus.PENDING)
                .ichStatus(IndustryVisit.ApprovalStatus.APPROVED)
                .principalStatus(IndustryVisit.ApprovalStatus.PENDING)
                .build();

        when(visitRepo.findById(706L)).thenReturn(Optional.of(visit));
        when(visitRepo.save(any(IndustryVisit.class))).thenAnswer(inv -> inv.getArgument(0));

        IndustryDto.VisitActionRequest req = IndustryDto.VisitActionRequest.builder()
                .status(IndustryVisit.ApprovalStatus.APPROVED)
                .remark("Visit to Bangalore requires Principal approval.")
                .build();

        IndustryDto.IndustryVisitDto result = industryService.hodVisitAction(706L, 400L, req, "hod_cse@tkmit.ac.in");

        assertNotNull(result);
        assertEquals(IndustryVisit.VisitStage.PRINCIPAL_REVIEW, result.getStage());
        assertEquals(IndustryVisit.VisitStatus.PENDING, result.getStatus());
        assertEquals(IndustryVisit.ApprovalStatus.PENDING, result.getPrincipalStatus());
    }

    @Test
    @DisplayName("Principal path: HOD approves large group > threshold -> escalates to PRINCIPAL_REVIEW")
    void testHodVisitAction_EscalatesToPrincipal_WhenLargeGroupExceedsThreshold() {
        industryService.setLargeGroupThreshold(50);

        IndustryVisit visit = IndustryVisit.builder()
                .id(707L)
                .company(testCompany)
                .proposedBy(testFaculty)
                .department("CSE")
                .date(LocalDate.now().plusDays(20))
                .studentCount(65) // 65 > 50 threshold
                .overnight(false)
                .outsideKerala(false)
                .stage(IndustryVisit.VisitStage.HOD_REVIEW)
                .status(IndustryVisit.VisitStatus.PENDING)
                .ichStatus(IndustryVisit.ApprovalStatus.APPROVED)
                .principalStatus(IndustryVisit.ApprovalStatus.PENDING)
                .build();

        when(visitRepo.findById(707L)).thenReturn(Optional.of(visit));
        when(visitRepo.save(any(IndustryVisit.class))).thenAnswer(inv -> inv.getArgument(0));

        IndustryDto.VisitActionRequest req = IndustryDto.VisitActionRequest.builder()
                .status(IndustryVisit.ApprovalStatus.APPROVED)
                .remark("Batch of 65 students requires Principal sanction.")
                .build();

        IndustryDto.IndustryVisitDto result = industryService.hodVisitAction(707L, 400L, req, "hod_cse@tkmit.ac.in");

        assertNotNull(result);
        assertEquals(IndustryVisit.VisitStage.PRINCIPAL_REVIEW, result.getStage());
        assertEquals(IndustryVisit.VisitStatus.PENDING, result.getStatus());
        assertEquals(IndustryVisit.ApprovalStatus.PENDING, result.getPrincipalStatus());
    }

    @Test
    @DisplayName("HOD reject marks visit COMPLETED and REJECTED")
    void testHodVisitAction_Reject_MarksCompletedAndRejected() {
        IndustryVisit visit = IndustryVisit.builder()
                .id(708L)
                .company(testCompany)
                .proposedBy(testFaculty)
                .department("CSE")
                .date(LocalDate.now().plusDays(20))
                .studentCount(35)
                .stage(IndustryVisit.VisitStage.HOD_REVIEW)
                .status(IndustryVisit.VisitStatus.PENDING)
                .build();

        when(visitRepo.findById(708L)).thenReturn(Optional.of(visit));
        when(visitRepo.save(any(IndustryVisit.class))).thenAnswer(inv -> inv.getArgument(0));

        IndustryDto.VisitActionRequest req = IndustryDto.VisitActionRequest.builder()
                .status(IndustryVisit.ApprovalStatus.REJECTED)
                .remark("Lab syllabus pending; cannot sanction day leave.")
                .build();

        IndustryDto.IndustryVisitDto result = industryService.hodVisitAction(708L, 400L, req, "hod_cse@tkmit.ac.in");

        assertNotNull(result);
        assertEquals(IndustryVisit.VisitStage.COMPLETED, result.getStage());
        assertEquals(IndustryVisit.VisitStatus.REJECTED, result.getStatus());
        assertEquals(IndustryVisit.ApprovalStatus.REJECTED, result.getHodStatus());
        verify(notificationService, times(1)).sendToUser(eq(300L), any(NotificationDto.class));
    }

    @Test
    @DisplayName("Principal path: Principal approves -> visit APPROVED and stage COMPLETED")
    void testPrincipalVisitAction_Approves_SanctionsVisit() {
        IndustryVisit visit = IndustryVisit.builder()
                .id(709L)
                .company(testCompany)
                .proposedBy(testFaculty)
                .department("CSE")
                .date(LocalDate.now().plusDays(30))
                .studentCount(60)
                .overnight(true)
                .stage(IndustryVisit.VisitStage.PRINCIPAL_REVIEW)
                .status(IndustryVisit.VisitStatus.PENDING)
                .ichStatus(IndustryVisit.ApprovalStatus.APPROVED)
                .hodStatus(IndustryVisit.ApprovalStatus.APPROVED)
                .principalStatus(IndustryVisit.ApprovalStatus.PENDING)
                .build();

        when(visitRepo.findById(709L)).thenReturn(Optional.of(visit));
        when(visitRepo.save(any(IndustryVisit.class))).thenAnswer(inv -> inv.getArgument(0));

        IndustryDto.VisitActionRequest req = IndustryDto.VisitActionRequest.builder()
                .status(IndustryVisit.ApprovalStatus.APPROVED)
                .remark("Sanctioned. Ensure two accompanying faculty members.")
                .build();

        IndustryDto.IndustryVisitDto result = industryService.principalVisitAction(709L, req, "principal@tkmit.ac.in");

        assertNotNull(result);
        assertEquals(IndustryVisit.VisitStage.COMPLETED, result.getStage());
        assertEquals(IndustryVisit.VisitStatus.APPROVED, result.getStatus());
        assertEquals(IndustryVisit.ApprovalStatus.APPROVED, result.getPrincipalStatus());
        assertNotNull(result.getPdfPath());
        verify(notificationService, times(1)).sendToUser(eq(300L), any(NotificationDto.class));
        verify(activityLogRepo, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("Principal path: Principal rejects -> visit REJECTED and stage COMPLETED")
    void testPrincipalVisitAction_Rejects_MarksCompletedAndRejected() {
        IndustryVisit visit = IndustryVisit.builder()
                .id(710L)
                .company(testCompany)
                .proposedBy(testFaculty)
                .department("CSE")
                .date(LocalDate.now().plusDays(30))
                .studentCount(60)
                .stage(IndustryVisit.VisitStage.PRINCIPAL_REVIEW)
                .status(IndustryVisit.VisitStatus.PENDING)
                .build();

        when(visitRepo.findById(710L)).thenReturn(Optional.of(visit));
        when(visitRepo.save(any(IndustryVisit.class))).thenAnswer(inv -> inv.getArgument(0));

        IndustryDto.VisitActionRequest req = IndustryDto.VisitActionRequest.builder()
                .status(IndustryVisit.ApprovalStatus.REJECTED)
                .remark("Dates conflict with KTU university end-semester examinations.")
                .build();

        IndustryDto.IndustryVisitDto result = industryService.principalVisitAction(710L, req, "principal@tkmit.ac.in");

        assertNotNull(result);
        assertEquals(IndustryVisit.VisitStage.COMPLETED, result.getStage());
        assertEquals(IndustryVisit.VisitStatus.REJECTED, result.getStatus());
        assertEquals(IndustryVisit.ApprovalStatus.REJECTED, result.getPrincipalStatus());
        verify(notificationService, times(1)).sendToUser(eq(300L), any(NotificationDto.class));
    }

    @Test
    @DisplayName("GET visits is scoped by role: faculty sees own, HOD sees dept, Principal sees all")
    void testGetVisits_ScopedByRole() {
        IndustryVisit visitCse = IndustryVisit.builder()
                .id(711L)
                .company(testCompany)
                .proposedBy(testFaculty)
                .department("CSE")
                .stage(IndustryVisit.VisitStage.ICH_REVIEW)
                .status(IndustryVisit.VisitStatus.PENDING)
                .build();

        when(visitRepo.findByProposedByIdOrderByCreatedAtDesc(300L)).thenReturn(List.of(visitCse));
        when(visitRepo.findByDepartmentOrderByCreatedAtDesc("CSE")).thenReturn(List.of(visitCse));
        when(visitRepo.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(visitCse));

        // Faculty
        List<IndustryDto.IndustryVisitDto> facultyVisits = industryService.getVisits(testFaculty, null, null, null);
        assertEquals(1, facultyVisits.size());
        verify(visitRepo, times(1)).findByProposedByIdOrderByCreatedAtDesc(300L);

        // HOD
        User hod = User.builder().id(400L).role(User.Role.HOD).department("CSE").build();
        List<IndustryDto.IndustryVisitDto> hodVisits = industryService.getVisits(hod, null, null, null);
        assertEquals(1, hodVisits.size());
        verify(visitRepo, times(1)).findByDepartmentOrderByCreatedAtDesc("CSE");

        // Principal
        User principal = User.builder().id(500L).role(User.Role.PRINCIPAL).build();
        List<IndustryDto.IndustryVisitDto> principalVisits = industryService.getVisits(principal, null, null, null);
        assertEquals(1, principalVisits.size());
        verify(visitRepo, times(1)).findAllByOrderByCreatedAtDesc();
    }

    // =========================================================================
    // 7. GUEST LECTURES CRUD & FILTERING TESTS
    // =========================================================================

    @Test
    @DisplayName("Should create guest lecture with company and coordinator")
    void testCreateLecture() {
        when(companyRepo.findById(1L)).thenReturn(Optional.of(testCompany));
        when(userRepo.findById(300L)).thenReturn(Optional.of(testFaculty));
        when(guestLectureRepo.save(any(GuestLecture.class))).thenAnswer(inv -> {
            GuestLecture gl = inv.getArgument(0);
            gl.setId(801L);
            return gl;
        });

        IndustryDto.GuestLectureRequest req = IndustryDto.GuestLectureRequest.builder()
                .companyId(1L)
                .coordinatorId(300L)
                .speaker("Dr. Rajesh Kumar")
                .designation("Principal Architect")
                .topic("Cloud-Native Microservices")
                .date(LocalDate.of(2026, 11, 20))
                .time("10:30 AM")
                .venue("Seminar Hall 1")
                .departments("CSE,ECE")
                .attendance(120)
                .feedbackAvg(4.8)
                .status(GuestLecture.LectureStatus.UPCOMING)
                .build();

        IndustryDto.GuestLectureDto dto = industryService.createLecture(req, "smitha.cse@tkmit.ac.in");

        assertNotNull(dto);
        assertEquals(801L, dto.getId());
        assertEquals("Dr. Rajesh Kumar", dto.getSpeaker());
        assertEquals("TechCorp Solutions", dto.getCompanyName());
        assertEquals("Cloud-Native Microservices", dto.getTopic());
        assertEquals("Dr. Smitha S", dto.getCoordinatorName());
        assertEquals(GuestLecture.LectureStatus.UPCOMING, dto.getStatus());
        verify(guestLectureRepo, times(1)).save(any(GuestLecture.class));
        verify(activityLogRepo, times(1)).save(any(IndustryActivityLog.class));
    }

    @Test
    @DisplayName("Should filter guest lectures by status and department")
    void testGetLectures_FilterByStatusAndDepartment() {
        GuestLecture l1 = GuestLecture.builder()
                .id(810L)
                .topic("Microservices")
                .status(GuestLecture.LectureStatus.UPCOMING)
                .departments("CSE,ECE")
                .date(LocalDate.of(2026, 11, 20))
                .build();
        GuestLecture l2 = GuestLecture.builder()
                .id(811L)
                .topic("Mechanical Robotics")
                .status(GuestLecture.LectureStatus.COMPLETED)
                .departments("ME")
                .date(LocalDate.of(2026, 10, 1))
                .build();

        when(guestLectureRepo.findAllByOrderByDateDesc()).thenReturn(List.of(l1, l2));

        List<IndustryDto.GuestLectureDto> cseUpcoming = industryService.getLectures(GuestLecture.LectureStatus.UPCOMING, "CSE");
        assertEquals(1, cseUpcoming.size());
        assertEquals("Microservices", cseUpcoming.get(0).getTopic());

        List<IndustryDto.GuestLectureDto> allUpcoming = industryService.getLectures(GuestLecture.LectureStatus.UPCOMING, null);
        assertEquals(1, allUpcoming.size());

        List<IndustryDto.GuestLectureDto> allLectures = industryService.getLectures(null, null);
        assertEquals(2, allLectures.size());
    }

    @Test
    @DisplayName("Should update existing guest lecture")
    void testUpdateLecture() {
        GuestLecture existing = GuestLecture.builder()
                .id(820L)
                .topic("Old Topic")
                .speaker("Speaker A")
                .status(GuestLecture.LectureStatus.UPCOMING)
                .departments("CSE")
                .build();

        when(guestLectureRepo.findById(820L)).thenReturn(Optional.of(existing));
        when(guestLectureRepo.save(any(GuestLecture.class))).thenAnswer(inv -> inv.getArgument(0));

        IndustryDto.GuestLectureRequest updateReq = IndustryDto.GuestLectureRequest.builder()
                .topic("Updated Topic on AI/ML")
                .status(GuestLecture.LectureStatus.COMPLETED)
                .attendance(150)
                .feedbackAvg(4.9)
                .build();

        IndustryDto.GuestLectureDto updated = industryService.updateLecture(820L, updateReq, "admin@tkmit.ac.in");

        assertNotNull(updated);
        assertEquals("Updated Topic on AI/ML", updated.getTopic());
        assertEquals(GuestLecture.LectureStatus.COMPLETED, updated.getStatus());
        assertEquals(150, updated.getAttendance());
        assertEquals(4.9, updated.getFeedbackAvg());
        verify(guestLectureRepo, times(1)).save(existing);
    }

    // =========================================================================
    // 8. INDUSTRY PROJECTS CRUD & FILTERING TESTS
    // =========================================================================

    @Test
    @DisplayName("Should create industry project with company")
    void testCreateProject() {
        when(companyRepo.findById(1L)).thenReturn(Optional.of(testCompany));
        when(industryProjectRepo.save(any(IndustryProject.class))).thenAnswer(inv -> {
            IndustryProject ip = inv.getArgument(0);
            ip.setId(901L);
            return ip;
        });

        IndustryDto.IndustryProjectRequest req = IndustryDto.IndustryProjectRequest.builder()
                .companyId(1L)
                .title("Smart Campus Energy Audit")
                .problemStatement("IoT based energy monitoring for college premises")
                .department("EEE")
                .mentor("Er. Anoop Kumar")
                .teamStudents("Kavya, Rahul, Vishnu")
                .status(IndustryProject.ProjectStatus.ONGOING)
                .academicYear("2026-2027")
                .build();

        IndustryDto.IndustryProjectDto dto = industryService.createProject(req, "faculty@tkmit.ac.in");

        assertNotNull(dto);
        assertEquals(901L, dto.getId());
        assertEquals("Smart Campus Energy Audit", dto.getTitle());
        assertEquals("TechCorp Solutions", dto.getCompanyName());
        assertEquals("EEE", dto.getDepartment());
        assertEquals(IndustryProject.ProjectStatus.ONGOING, dto.getStatus());
        verify(industryProjectRepo, times(1)).save(any(IndustryProject.class));
    }

    @Test
    @DisplayName("Should filter industry projects by status, department, and company")
    void testGetProjects_Filtering() {
        IndustryProject p1 = IndustryProject.builder()
                .id(910L)
                .company(testCompany)
                .title("Cloud IoT")
                .department("CSE")
                .status(IndustryProject.ProjectStatus.ONGOING)
                .build();
        IndustryProject p2 = IndustryProject.builder()
                .id(911L)
                .title("Bridge Sensor")
                .department("CE")
                .status(IndustryProject.ProjectStatus.COMPLETED)
                .build();

        when(industryProjectRepo.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(p1, p2));

        List<IndustryDto.IndustryProjectDto> cseActive = industryService.getProjects(IndustryProject.ProjectStatus.ONGOING, "CSE", 1L);
        assertEquals(1, cseActive.size());
        assertEquals("Cloud IoT", cseActive.get(0).getTitle());

        List<IndustryDto.IndustryProjectDto> ceProjects = industryService.getProjects(null, "CE", null);
        assertEquals(1, ceProjects.size());
        assertEquals("Bridge Sensor", ceProjects.get(0).getTitle());
    }

    @Test
    @DisplayName("Should update existing industry project")
    void testUpdateProject() {
        IndustryProject existing = IndustryProject.builder()
                .id(920L)
                .title("Old Title")
                .status(IndustryProject.ProjectStatus.PROPOSED)
                .department("ME")
                .build();

        when(industryProjectRepo.findById(920L)).thenReturn(Optional.of(existing));
        when(industryProjectRepo.save(any(IndustryProject.class))).thenAnswer(inv -> inv.getArgument(0));

        IndustryDto.IndustryProjectRequest updateReq = IndustryDto.IndustryProjectRequest.builder()
                .title("Autonomous Rover Chassis")
                .status(IndustryProject.ProjectStatus.COMPLETED)
                .outcome("Working prototype fabricated and published paper")
                .build();

        IndustryDto.IndustryProjectDto updated = industryService.updateProject(920L, updateReq, "admin@tkmit.ac.in");

        assertNotNull(updated);
        assertEquals("Autonomous Rover Chassis", updated.getTitle());
        assertEquals(IndustryProject.ProjectStatus.COMPLETED, updated.getStatus());
        assertEquals("Working prototype fabricated and published paper", updated.getOutcome());
    }

    // =========================================================================
    // 9. ADVISORY BOARD MEMBERS CRUD TESTS
    // =========================================================================

    @Test
    @DisplayName("Should create advisory board member")
    void testCreateAdvisoryMember() {
        when(companyRepo.findById(1L)).thenReturn(Optional.of(testCompany));
        when(advisoryMemberRepo.save(any(AdvisoryMember.class))).thenAnswer(inv -> {
            AdvisoryMember m = inv.getArgument(0);
            m.setId(1001L);
            return m;
        });

        IndustryDto.AdvisoryMemberRequest req = IndustryDto.AdvisoryMemberRequest.builder()
                .name("Sunil Nambiar")
                .designation("VP of Engineering")
                .companyId(1L)
                .email("sunil@techcorp.com")
                .phone("+91-9847012345")
                .domain("Cloud Computing & AI")
                .bio("20+ years of enterprise architecture experience")
                .tenureStart(LocalDate.of(2025, 1, 1))
                .tenureEnd(LocalDate.of(2027, 12, 31))
                .status(AdvisoryMember.MemberStatus.ACTIVE)
                .build();

        IndustryDto.AdvisoryMemberDto dto = industryService.createAdvisoryMember(req, "admin@tkmit.ac.in");

        assertNotNull(dto);
        assertEquals(1001L, dto.getId());
        assertEquals("Sunil Nambiar", dto.getName());
        assertEquals("TechCorp Solutions", dto.getCompanyName());
        assertEquals("Cloud Computing & AI", dto.getDomain());
        assertEquals(AdvisoryMember.MemberStatus.ACTIVE, dto.getStatus());
        verify(advisoryMemberRepo, times(1)).save(any(AdvisoryMember.class));
    }

    @Test
    @DisplayName("Should filter advisory members by status")
    void testGetAdvisoryMembers_FilterByStatus() {
        AdvisoryMember m1 = AdvisoryMember.builder().id(1010L).name("Member Active").status(AdvisoryMember.MemberStatus.ACTIVE).build();
        AdvisoryMember m2 = AdvisoryMember.builder().id(1011L).name("Member Inactive").status(AdvisoryMember.MemberStatus.INACTIVE).build();

        when(advisoryMemberRepo.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(m1, m2));

        List<IndustryDto.AdvisoryMemberDto> activeOnly = industryService.getAdvisoryMembers(AdvisoryMember.MemberStatus.ACTIVE);
        assertEquals(1, activeOnly.size());
        assertEquals("Member Active", activeOnly.get(0).getName());

        List<IndustryDto.AdvisoryMemberDto> allMembers = industryService.getAdvisoryMembers(null);
        assertEquals(2, allMembers.size());
    }

    @Test
    @DisplayName("Should update existing advisory member")
    void testUpdateAdvisoryMember() {
        AdvisoryMember existing = AdvisoryMember.builder()
                .id(1020L)
                .name("Sunil Nambiar")
                .designation("Director")
                .status(AdvisoryMember.MemberStatus.ACTIVE)
                .build();

        when(advisoryMemberRepo.findById(1020L)).thenReturn(Optional.of(existing));
        when(advisoryMemberRepo.save(any(AdvisoryMember.class))).thenAnswer(inv -> inv.getArgument(0));

        IndustryDto.AdvisoryMemberRequest updateReq = IndustryDto.AdvisoryMemberRequest.builder()
                .designation("Senior VP of Technology")
                .status(AdvisoryMember.MemberStatus.INACTIVE)
                .build();

        IndustryDto.AdvisoryMemberDto updated = industryService.updateAdvisoryMember(1020L, updateReq, "admin@tkmit.ac.in");

        assertNotNull(updated);
        assertEquals("Senior VP of Technology", updated.getDesignation());
        assertEquals(AdvisoryMember.MemberStatus.INACTIVE, updated.getStatus());
    }

    // =========================================================================
    // 10. ALUMNI CORPORATE CONTACTS CRUD TESTS
    // =========================================================================

    @Test
    @DisplayName("Should create alumni contact")
    void testCreateAlumniContact() {
        when(alumniContactRepo.save(any(AlumniContact.class))).thenAnswer(inv -> {
            AlumniContact ac = inv.getArgument(0);
            ac.setId(1101L);
            return ac;
        });

        IndustryDto.AlumniContactRequest req = IndustryDto.AlumniContactRequest.builder()
                .name("Deepa Menon")
                .batch("2022")
                .degree("B.Tech")
                .department("CSE")
                .currentCompany("TechCorp Solutions")
                .currentRole("Staff Software Engineer")
                .email("deepa@techcorp.com")
                .phone("+91-9447123456")
                .linkedinUrl("https://linkedin.com/in/deepa-menon")
                .willingToMentor(true)
                .willingToRefer(true)
                .build();

        IndustryDto.AlumniContactDto dto = industryService.createAlumniContact(req, "coordinator@tkmit.ac.in");

        assertNotNull(dto);
        assertEquals(1101L, dto.getId());
        assertEquals("Deepa Menon", dto.getName());
        assertEquals("2022", dto.getBatch());
        assertEquals("CSE", dto.getDepartment());
        assertEquals("TechCorp Solutions", dto.getCurrentCompany());
        assertTrue(dto.getWillingToMentor());
        assertTrue(dto.getWillingToRefer());
        verify(alumniContactRepo, times(1)).save(any(AlumniContact.class));
    }

    @Test
    @DisplayName("Should filter alumni contacts by department, company, mentor, and refer")
    void testGetAlumniContacts_Filtering() {
        AlumniContact a1 = AlumniContact.builder()
                .id(1110L)
                .name("Deepa Menon")
                .department("CSE")
                .currentCompany("TechCorp Solutions")
                .willingToMentor(true)
                .willingToRefer(true)
                .build();
        AlumniContact a2 = AlumniContact.builder()
                .id(1111L)
                .name("Rohan Nair")
                .department("ECE")
                .currentCompany("Intel")
                .willingToMentor(false)
                .willingToRefer(true)
                .build();

        when(alumniContactRepo.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(a1, a2));

        List<IndustryDto.AlumniContactDto> filtered = industryService.getAlumniContacts("CSE", "TechCorp", true, true);
        assertEquals(1, filtered.size());
        assertEquals("Deepa Menon", filtered.get(0).getName());

        List<IndustryDto.AlumniContactDto> eceList = industryService.getAlumniContacts("ECE", null, null, null);
        assertEquals(1, eceList.size());
        assertEquals("Rohan Nair", eceList.get(0).getName());
    }

    @Test
    @DisplayName("Should update existing alumni contact")
    void testUpdateAlumniContact() {
        AlumniContact existing = AlumniContact.builder()
                .id(1120L)
                .name("Deepa Menon")
                .currentRole("Senior Engineer")
                .willingToMentor(true)
                .willingToRefer(false)
                .build();

        when(alumniContactRepo.findById(1120L)).thenReturn(Optional.of(existing));
        when(alumniContactRepo.save(any(AlumniContact.class))).thenAnswer(inv -> inv.getArgument(0));

        IndustryDto.AlumniContactRequest updateReq = IndustryDto.AlumniContactRequest.builder()
                .currentRole("Engineering Manager")
                .willingToRefer(true)
                .build();

        IndustryDto.AlumniContactDto updated = industryService.updateAlumniContact(1120L, updateReq, "admin@tkmit.ac.in");

        assertNotNull(updated);
        assertEquals("Engineering Manager", updated.getCurrentRole());
        assertTrue(updated.getWillingToRefer());
    }

    // =========================================================================
    // 11. REPORTS SUMMARY & EXPORT TESTS
    // =========================================================================

    @Test
    @DisplayName("Should calculate department-wise engagement summary correctly")
    void testGetEngagementSummary() {
        when(mouRepo.findAll()).thenReturn(List.of(
                IndustryMou.builder().id(1L).departments("CSE,ECE").status(IndustryMou.MouStatus.ACTIVE).build()
        ));
        when(internshipRepo.findAll()).thenReturn(List.of(
                Internship.builder().id(10L).departments("CSE").status(Internship.InternshipStatus.OPEN).build()
        ));
        when(visitRepo.findAll()).thenReturn(List.of(
                IndustryVisit.builder().id(20L).department("CSE").build(),
                IndustryVisit.builder().id(21L).department("ME").build()
        ));
        when(guestLectureRepo.findAll()).thenReturn(List.of(
                GuestLecture.builder().id(30L).departments("CSE,AIDS").build()
        ));
        when(industryProjectRepo.findAll()).thenReturn(List.of(
                IndustryProject.builder().id(40L).department("CSE").build()
        ));

        IndustryDto.IndustryEngagementSummaryDto summary = industryService.getEngagementSummary();

        assertNotNull(summary);
        assertNotNull(summary.getTotals());
        assertEquals(1, summary.getTotals().getMous());
        assertEquals(1, summary.getTotals().getInternships());
        assertEquals(2, summary.getTotals().getVisits());
        assertEquals(1, summary.getTotals().getLectures());
        assertEquals(1, summary.getTotals().getProjects());
        assertEquals(6, summary.getTotals().getTotalEngagement());

        // Find CSE breakdown
        IndustryDto.DepartmentEngagementDto cse = summary.getDepartments().stream()
                .filter(d -> "CSE".equalsIgnoreCase(d.getDepartment()))
                .findFirst()
                .orElse(null);

        assertNotNull(cse);
        assertEquals(1, cse.getMous());
        assertEquals(1, cse.getInternships());
        assertEquals(1, cse.getVisits());
        assertEquals(1, cse.getLectures());
        assertEquals(1, cse.getProjects());
        assertEquals(5, cse.getTotalEngagement());
    }

    @Test
    @DisplayName("Should export MoUs, Internships, and Visits as CSV")
    void testExportCsv_MouInternshipVisit() {
        // 1. MoU CSV
        when(mouRepo.findAll()).thenReturn(List.of(
                IndustryMou.builder()
                        .id(1L)
                        .company(testCompany)
                        .title("Cloud Academy MoU")
                        .signedDate(LocalDate.of(2025, 1, 15))
                        .validFrom(LocalDate.of(2025, 1, 15))
                        .validTo(LocalDate.of(2028, 1, 15))
                        .status(IndustryMou.MouStatus.ACTIVE)
                        .departments("CSE,ECE")
                        .scope("Internships, Guest Lectures")
                        .build()
        ));

        byte[] mouCsvBytes = industryService.exportCsv("mou");
        assertNotNull(mouCsvBytes);
        String mouCsv = new String(mouCsvBytes, StandardCharsets.UTF_8);
        assertTrue(mouCsv.startsWith("ID,Company,Title,SignedDate,ValidFrom,ValidTo,Status,Departments,Scope"));
        assertTrue(mouCsv.contains("Cloud Academy MoU"));
        assertTrue(mouCsv.contains("TechCorp Solutions"));

        // 2. Internship CSV
        when(internshipRepo.findAll()).thenReturn(List.of(
                Internship.builder()
                        .id(50L)
                        .company(testCompany)
                        .title("Cloud DevOps Intern")
                        .duration("3 Months")
                        .stipend("15000/month")
                        .mode("HYBRID")
                        .seats(4)
                        .deadline(LocalDate.of(2026, 12, 1))
                        .status(Internship.InternshipStatus.OPEN)
                        .departments("CSE")
                        .build()
        ));

        byte[] internCsvBytes = industryService.exportCsv("internship");
        assertNotNull(internCsvBytes);
        String internCsv = new String(internCsvBytes, StandardCharsets.UTF_8);
        assertTrue(internCsv.startsWith("ID,Company,Title,Duration,Stipend,Mode,Seats,Deadline,Status,Departments"));
        assertTrue(internCsv.contains("Cloud DevOps Intern"));
        assertTrue(internCsv.contains("HYBRID"));

        // 3. Visit CSV
        when(visitRepo.findAll()).thenReturn(List.of(
                IndustryVisit.builder()
                        .id(700L)
                        .company(testCompany)
                        .department("CSE")
                        .proposedBy(testFaculty)
                        .date(LocalDate.of(2026, 11, 10))
                        .studentCount(45)
                        .stage(IndustryVisit.VisitStage.COMPLETED)
                        .status(IndustryVisit.VisitStatus.APPROVED)
                        .location("Infopark Kochi")
                        .build()
        ));

        byte[] visitCsvBytes = industryService.exportCsv("visit");
        assertNotNull(visitCsvBytes);
        String visitCsv = new String(visitCsvBytes, StandardCharsets.UTF_8);
        assertTrue(visitCsv.startsWith("ID,Company,Department,Proposer,VisitDate,StudentCount,Stage,Status,Location"));
        assertTrue(visitCsv.contains("Dr. Smitha S"));
        assertTrue(visitCsv.contains("Infopark Kochi"));
    }

    @Test
    @DisplayName("Should throw BadRequestException on unsupported export CSV type")
    void testExportCsv_InvalidType() {
        assertThrows(BadRequestException.class, () -> industryService.exportCsv("unknown_type"));
        assertThrows(BadRequestException.class, () -> industryService.exportCsv(null));
        assertThrows(BadRequestException.class, () -> industryService.exportCsv("   "));
    }

    @Test
    @DisplayName("Should export reports as PDF delegating to PdfService")
    void testExportPdf_DelegatesToPdfService() {
        when(mouRepo.findAll()).thenReturn(List.of());
        when(internshipRepo.findAll()).thenReturn(List.of());
        when(visitRepo.findAll()).thenReturn(List.of());

        byte[] dummyMouPdf = new byte[]{1, 2, 3};
        byte[] dummyInternshipPdf = new byte[]{4, 5, 6};
        byte[] dummyVisitPdf = new byte[]{7, 8, 9};

        when(pdfService.generateIndustryReportPdf(eq("mou"), any())).thenReturn(dummyMouPdf);
        when(pdfService.generateIndustryReportPdf(eq("internship"), any())).thenReturn(dummyInternshipPdf);
        when(pdfService.generateIndustryReportPdf(eq("visit"), any())).thenReturn(dummyVisitPdf);

        assertArrayEquals(dummyMouPdf, industryService.exportPdf("mou"));
        assertArrayEquals(dummyInternshipPdf, industryService.exportPdf("internship"));
        assertArrayEquals(dummyVisitPdf, industryService.exportPdf("visit"));

        assertThrows(BadRequestException.class, () -> industryService.exportPdf("unknown"));
        assertThrows(BadRequestException.class, () -> industryService.exportPdf(""));
    }
}

