package com.edusync.controller;

import com.edusync.dto.AppDto;
import com.edusync.dto.IndustryDto.*;
import com.edusync.exception.ResourceNotFoundException;
import com.edusync.model.*;
import com.edusync.repository.UserRepository;
import com.edusync.security.JwtUtil;
import com.edusync.service.IndustryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controller for Industrial Connect Head (ICH) & Industry Partnership layer.
 * Secured with specific matchers for open internships and student applications,
 * and restricted to INDUSTRIAL_CONNECT_HEAD and ADMIN roles for management APIs.
 */
@RestController
@RequestMapping("/api/industry")
@RequiredArgsConstructor
@CrossOrigin
public class IndustryController {

    private final IndustryService industryService;
    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;

    // =========================================================================
    // 1. KPI OVERVIEW
    // =========================================================================

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','PRINCIPAL','HOD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<IndustryOverviewDto>> getOverview() {
        IndustryOverviewDto overview = industryService.getOverview();
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Industrial Connect Overview KPIs", overview));
    }

    // =========================================================================
    // 2. COMPANIES MANAGEMENT
    // =========================================================================

    @GetMapping("/companies")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','PLACEMENT_OFFICER','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<List<CompanyDto>>> getCompanies(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Company.PartnershipStage stage,
            @RequestParam(required = false) Boolean placementReady
    ) {
        List<CompanyDto> companies = industryService.getCompanies(search, stage, placementReady);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Fetched partner companies", companies));
    }

    @GetMapping("/companies/{id}")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<CompanyDto>> getCompanyById(@PathVariable Long id) {
        CompanyDto company = industryService.getCompanyById(id);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Fetched company details", company));
    }

    @PostMapping("/companies")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<CompanyDto>> createCompany(
            @Valid @RequestBody CompanyRequest request,
            Authentication auth
    ) {
        String actor = resolveActor(auth);
        CompanyDto created = industryService.createCompany(request, actor);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Company created successfully", created));
    }

    @PutMapping("/companies/{id}")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<CompanyDto>> updateCompany(
            @PathVariable Long id,
            @Valid @RequestBody CompanyRequest request,
            Authentication auth
    ) {
        String actor = resolveActor(auth);
        CompanyDto updated = industryService.updateCompany(id, request, actor);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Company updated successfully", updated));
    }

    @PutMapping("/companies/{id}/stage")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<CompanyDto>> updateCompanyStage(
            @PathVariable Long id,
            @Valid @RequestBody CompanyStageUpdateRequest request,
            Authentication auth
    ) {
        String actor = resolveActor(auth);
        CompanyDto updated = industryService.updateCompanyStage(id, request, actor);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Company partnership stage updated", updated));
    }

    // =========================================================================
    // 3. MOUs MANAGEMENT & RENEWALS
    // =========================================================================

    @GetMapping("/mous")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<List<IndustryMouDto>>> getMous(
            @RequestParam(required = false) IndustryMou.MouStatus status,
            @RequestParam(required = false) Long companyId
    ) {
        List<IndustryMouDto> mous = industryService.getMous(status, companyId);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Fetched MoUs", mous));
    }

    @GetMapping("/mous/expiring")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<List<IndustryMouDto>>> getExpiringMous() {
        List<IndustryMouDto> expiring = industryService.getExpiringMous();
        return ResponseEntity.ok(AppDto.ApiResponse.ok("MoUs expiring within 60 days", expiring));
    }

    @GetMapping("/mous/{id}")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<IndustryMouDto>> getMouById(@PathVariable Long id) {
        IndustryMouDto mou = industryService.getMouById(id);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Fetched MoU details", mou));
    }

    @PostMapping("/mous")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<IndustryMouDto>> createMou(
            @Valid @RequestBody IndustryMouRequest request,
            Authentication auth
    ) {
        String actor = resolveActor(auth);
        IndustryMouDto created = industryService.createMou(request, actor);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("MoU created successfully", created));
    }

    @PutMapping("/mous/{id}")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<IndustryMouDto>> updateMou(
            @PathVariable Long id,
            @Valid @RequestBody IndustryMouRequest request,
            Authentication auth
    ) {
        String actor = resolveActor(auth);
        IndustryMouDto updated = industryService.updateMou(id, request, actor);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("MoU updated successfully", updated));
    }

    @PutMapping("/mous/{id}/renew")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<IndustryMouDto>> renewMou(
            @PathVariable Long id,
            @Valid @RequestBody MouRenewalRequest request,
            Authentication auth
    ) {
        String actor = resolveActor(auth);
        IndustryMouDto renewed = industryService.renewMou(id, request, actor);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("MoU renewed successfully", renewed));
    }

    @PostMapping(value = "/mous/{id}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<IndustryMouDto>> uploadMouDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication auth
    ) {
        String actor = resolveActor(auth);
        IndustryMouDto updated = industryService.uploadMouDocument(id, file, actor);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Signed MoU uploaded to Google Drive", updated));
    }

    @PostMapping("/mous/check-expirations")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<Integer>> triggerManualExpirationCheck() {
        int count = industryService.processDailyMouExpirations();
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Processed daily expiration cycle", count));
    }

    // =========================================================================
    // 4. INTERNSHIPS & APPLICATIONS
    // =========================================================================

    @GetMapping("/internships")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<List<InternshipDto>>> getInternships(
            @RequestParam(required = false) Internship.InternshipStatus status,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String department
    ) {
        List<InternshipDto> list = industryService.getInternships(status, companyId, department);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Fetched internships", list));
    }

    @GetMapping("/internships/{id}")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<InternshipDto>> getInternshipById(@PathVariable Long id) {
        InternshipDto internship = industryService.getInternshipById(id);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Fetched internship details", internship));
    }

    @PostMapping("/internships")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<InternshipDto>> createInternship(
            @Valid @RequestBody InternshipRequest request,
            Authentication auth
    ) {
        String actor = resolveActor(auth);
        InternshipDto created = industryService.createInternship(request, actor);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Internship opportunity created", created));
    }

    @PutMapping("/internships/{id}")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<InternshipDto>> updateInternship(
            @PathVariable Long id,
            @Valid @RequestBody InternshipRequest request,
            Authentication auth
    ) {
        String actor = resolveActor(auth);
        InternshipDto updated = industryService.updateInternship(id, request, actor);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Internship opportunity updated", updated));
    }

    @GetMapping("/internships/{id}/applications")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<List<InternshipApplicationDto>>> getInternshipApplications(
            @PathVariable Long id
    ) {
        List<InternshipApplicationDto> apps = industryService.getInternshipApplications(id);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Fetched internship applications", apps));
    }

    @PutMapping("/applications/{id}/status")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<InternshipApplicationDto>> updateApplicationStatus(
            @PathVariable Long id,
            @Valid @RequestBody InternshipApplicationStatusRequest request,
            Authentication auth
    ) {
        String actor = resolveActor(auth);
        InternshipApplicationDto updated = industryService.updateApplicationStatus(id, request, actor);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Internship application status updated", updated));
    }

    @PostMapping(value = "/applications/{id}/certificate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<InternshipApplicationDto>> uploadApplicationCertificate(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Authentication auth
    ) {
        String actor = resolveActor(auth);
        InternshipApplicationDto updated = industryService.uploadApplicationCertificate(id, file, actor);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Internship completion certificate uploaded", updated));
    }

    // =========================================================================
    // 5. STUDENT ENDPOINTS
    // =========================================================================

    /**
     * Student view of open internships:
     * - Only ACTIVE / OPEN internships
     * - Before the application deadline
     * - Matching the student's department
     * - Response does NOT include company HR contact details
     */
    @GetMapping("/internships/open")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AppDto.ApiResponse<List<InternshipDto>>> getOpenInternshipsForStudent(
            Authentication auth
    ) {
        String studentDept = null;
        if (auth != null && auth.getName() != null) {
            studentDept = userRepo.findByEmailIgnoreCase(auth.getName())
                    .map(User::getDepartment)
                    .orElse(null);
        }

        List<InternshipDto> openList = industryService.getOpenInternshipsForStudent(studentDept);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Active open internships", openList));
    }

    /**
     * Student applies to an internship:
     * - Enforces deadline
     * - Blocks duplicate applications
     * - Enforces seat limits
     * - Dispatches FCM notification to student
     */
    @PostMapping("/internships/{id}/apply")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AppDto.ApiResponse<InternshipApplicationDto>> applyForInternship(
            @PathVariable Long id,
            @RequestBody(required = false) InternshipApplicationRequest request,
            Authentication auth
    ) {
        User student = userRepo.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Student user not found for email: " + auth.getName()));

        InternshipApplicationDto application = industryService.applyForInternship(id, student.getId(), request, student.getEmail());
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Internship application submitted successfully", application));
    }

    @GetMapping("/internships/my-applications")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AppDto.ApiResponse<List<InternshipApplicationDto>>> getMyApplications(Authentication auth) {
        User student = userRepo.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Student user not found for email: " + auth.getName()));
        List<InternshipApplicationDto> apps = industryService.getStudentApplications(student.getId());
        return ResponseEntity.ok(AppDto.ApiResponse.ok("My internship applications", apps));
    }

    // =========================================================================
    // 6. INDUSTRIAL VISITS APPROVAL FLOW
    // =========================================================================

    @PostMapping("/visits/propose")
    @PreAuthorize("hasAnyRole('FACULTY','HOD')")
    public ResponseEntity<AppDto.ApiResponse<IndustryVisitDto>> proposeVisit(
            @Valid @RequestBody IndustryVisitRequest request,
            Authentication auth
    ) {
        User proposer = resolveUser(auth);
        String actor = resolveActor(auth);
        IndustryVisitDto created = industryService.proposeVisit(request, proposer != null ? proposer.getId() : null, actor);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Industrial visit proposed successfully", created));
    }

    @PutMapping("/visits/{id}/ich-action")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<IndustryVisitDto>> ichVisitAction(
            @PathVariable Long id,
            @Valid @RequestBody VisitActionRequest request,
            Authentication auth
    ) {
        String actor = resolveActor(auth);
        IndustryVisitDto updated = industryService.ichVisitAction(id, request, actor);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("ICH visit action recorded", updated));
    }

    @PutMapping("/visits/{id}/hod-action")
    @PreAuthorize("hasAnyRole('HOD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<IndustryVisitDto>> hodVisitAction(
            @PathVariable Long id,
            @Valid @RequestBody VisitActionRequest request,
            Authentication auth
    ) {
        User hodUser = resolveUser(auth);
        String actor = resolveActor(auth);
        Long hodId = hodUser != null ? hodUser.getId() : null;
        IndustryVisitDto updated = industryService.hodVisitAction(id, hodId, request, actor);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("HOD visit action recorded", updated));
    }

    @PutMapping("/visits/{id}/principal-action")
    @PreAuthorize("hasAnyRole('PRINCIPAL','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<IndustryVisitDto>> principalVisitAction(
            @PathVariable Long id,
            @Valid @RequestBody VisitActionRequest request,
            Authentication auth
    ) {
        String actor = resolveActor(auth);
        IndustryVisitDto updated = industryService.principalVisitAction(id, request, actor);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Principal visit action recorded", updated));
    }

    @GetMapping("/visits")
    @PreAuthorize("hasAnyRole('FACULTY','HOD','PRINCIPAL','INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<List<IndustryVisitDto>>> getVisits(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) IndustryVisit.VisitStage stage,
            @RequestParam(required = false) IndustryVisit.VisitStatus status,
            Authentication auth
    ) {
        User currentUser = resolveUser(auth);
        List<IndustryVisitDto> visits = industryService.getVisits(currentUser, department, stage, status);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Industrial visits fetched successfully", visits));
    }

    @GetMapping("/visits/{id}")
    @PreAuthorize("hasAnyRole('FACULTY','HOD','PRINCIPAL','INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<IndustryVisitDto>> getVisitById(@PathVariable Long id) {
        IndustryVisitDto visit = industryService.getVisitById(id);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Industrial visit details", visit));
    }

    // =========================================================================
    // 7. GUEST LECTURES
    // =========================================================================

    @GetMapping("/lectures")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN','FACULTY','HOD','PRINCIPAL')")
    public ResponseEntity<AppDto.ApiResponse<List<GuestLectureDto>>> getLectures(
            @RequestParam(required = false) GuestLecture.LectureStatus status,
            @RequestParam(required = false) String department
    ) {
        List<GuestLectureDto> list = industryService.getLectures(status, department);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Fetched guest lectures", list));
    }

    @GetMapping("/lectures/{id}")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN','FACULTY','HOD','PRINCIPAL')")
    public ResponseEntity<AppDto.ApiResponse<GuestLectureDto>> getLectureById(@PathVariable Long id) {
        GuestLectureDto lecture = industryService.getLectureById(id);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Fetched guest lecture details", lecture));
    }

    @PostMapping("/lectures")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<GuestLectureDto>> createLecture(
            @Valid @RequestBody GuestLectureRequest request,
            Authentication auth
    ) {
        String actor = resolveActor(auth);
        GuestLectureDto created = industryService.createLecture(request, actor);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Guest lecture created successfully", created));
    }

    @PutMapping("/lectures/{id}")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<GuestLectureDto>> updateLecture(
            @PathVariable Long id,
            @Valid @RequestBody GuestLectureRequest request,
            Authentication auth
    ) {
        String actor = resolveActor(auth);
        GuestLectureDto updated = industryService.updateLecture(id, request, actor);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Guest lecture updated successfully", updated));
    }

    // =========================================================================
    // 8. INDUSTRY PROJECTS
    // =========================================================================

    @GetMapping("/projects")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN','FACULTY','HOD','PRINCIPAL')")
    public ResponseEntity<AppDto.ApiResponse<List<IndustryProjectDto>>> getProjects(
            @RequestParam(required = false) IndustryProject.ProjectStatus status,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Long companyId
    ) {
        List<IndustryProjectDto> list = industryService.getProjects(status, department, companyId);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Fetched industry projects", list));
    }

    @GetMapping("/projects/{id}")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN','FACULTY','HOD','PRINCIPAL')")
    public ResponseEntity<AppDto.ApiResponse<IndustryProjectDto>> getProjectById(@PathVariable Long id) {
        IndustryProjectDto project = industryService.getProjectById(id);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Fetched industry project details", project));
    }

    @PostMapping("/projects")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<IndustryProjectDto>> createProject(
            @Valid @RequestBody IndustryProjectRequest request,
            Authentication auth
    ) {
        String actor = resolveActor(auth);
        IndustryProjectDto created = industryService.createProject(request, actor);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Industry project created successfully", created));
    }

    @PutMapping("/projects/{id}")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<IndustryProjectDto>> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody IndustryProjectRequest request,
            Authentication auth
    ) {
        String actor = resolveActor(auth);
        IndustryProjectDto updated = industryService.updateProject(id, request, actor);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Industry project updated successfully", updated));
    }

    // =========================================================================
    // 9. ADVISORY BOARD MEMBERS
    // =========================================================================

    @GetMapping("/advisory")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN','FACULTY','HOD','PRINCIPAL')")
    public ResponseEntity<AppDto.ApiResponse<List<AdvisoryMemberDto>>> getAdvisoryMembers(
            @RequestParam(required = false) AdvisoryMember.MemberStatus status
    ) {
        List<AdvisoryMemberDto> list = industryService.getAdvisoryMembers(status);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Fetched advisory board members", list));
    }

    @GetMapping("/advisory/{id}")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN','FACULTY','HOD','PRINCIPAL')")
    public ResponseEntity<AppDto.ApiResponse<AdvisoryMemberDto>> getAdvisoryMemberById(@PathVariable Long id) {
        AdvisoryMemberDto member = industryService.getAdvisoryMemberById(id);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Fetched advisory member details", member));
    }

    @PostMapping("/advisory")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<AdvisoryMemberDto>> createAdvisoryMember(
            @Valid @RequestBody AdvisoryMemberRequest request,
            Authentication auth
    ) {
        String actor = resolveActor(auth);
        AdvisoryMemberDto created = industryService.createAdvisoryMember(request, actor);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Advisory member created successfully", created));
    }

    @PutMapping("/advisory/{id}")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<AdvisoryMemberDto>> updateAdvisoryMember(
            @PathVariable Long id,
            @Valid @RequestBody AdvisoryMemberRequest request,
            Authentication auth
    ) {
        String actor = resolveActor(auth);
        AdvisoryMemberDto updated = industryService.updateAdvisoryMember(id, request, actor);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Advisory member updated successfully", updated));
    }

    // =========================================================================
    // 10. ALUMNI CORPORATE NETWORK
    // =========================================================================

    @GetMapping("/alumni")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN','FACULTY','HOD','PRINCIPAL')")
    public ResponseEntity<AppDto.ApiResponse<List<AlumniContactDto>>> getAlumniContacts(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) Boolean willingToMentor,
            @RequestParam(required = false) Boolean willingToRefer
    ) {
        List<AlumniContactDto> list = industryService.getAlumniContacts(department, company, willingToMentor, willingToRefer);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Fetched corporate alumni contacts", list));
    }

    @GetMapping("/alumni/{id}")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN','FACULTY','HOD','PRINCIPAL')")
    public ResponseEntity<AppDto.ApiResponse<AlumniContactDto>> getAlumniContactById(@PathVariable Long id) {
        AlumniContactDto contact = industryService.getAlumniContactById(id);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Fetched alumni contact details", contact));
    }

    @PostMapping("/alumni")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<AlumniContactDto>> createAlumniContact(
            @Valid @RequestBody AlumniContactRequest request,
            Authentication auth
    ) {
        String actor = resolveActor(auth);
        AlumniContactDto created = industryService.createAlumniContact(request, actor);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Alumni contact created successfully", created));
    }

    @PutMapping("/alumni/{id}")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<AlumniContactDto>> updateAlumniContact(
            @PathVariable Long id,
            @Valid @RequestBody AlumniContactRequest request,
            Authentication auth
    ) {
        String actor = resolveActor(auth);
        AlumniContactDto updated = industryService.updateAlumniContact(id, request, actor);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Alumni contact updated successfully", updated));
    }

    // =========================================================================
    // 11. REPORTS & ENGAGEMENT ANALYTICS
    // =========================================================================

    @GetMapping("/reports/summary")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','PRINCIPAL','HOD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<IndustryEngagementSummaryDto>> getEngagementSummary() {
        IndustryEngagementSummaryDto summary = industryService.getEngagementSummary();
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Department-wise industry engagement summary", summary));
    }

    @GetMapping("/reports/export")
    @PreAuthorize("hasAnyRole('INDUSTRIAL_CONNECT_HEAD','PRINCIPAL','HOD','ADMIN')")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam String type,
            @RequestParam(required = false, defaultValue = "csv") String format
    ) {
        if ("pdf".equalsIgnoreCase(format)) {
            byte[] pdf = industryService.exportPdf(type);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"industry-" + type.toLowerCase() + "-report.pdf\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } else {
            byte[] csv = industryService.exportCsv(type);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"industry-" + type.toLowerCase() + "-export.csv\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(csv);
        }
    }

    // =========================================================================
    // HELPER
    // =========================================================================

    private String resolveActor(Authentication auth) {
        if (auth != null && auth.getName() != null && !auth.getName().trim().isEmpty()) {
            return auth.getName();
        }
        return "ICH_USER";
    }

    private User resolveUser(Authentication auth) {
        if (auth != null && auth.getName() != null) {
            return userRepo.findByEmailIgnoreCase(auth.getName()).orElse(null);
        }
        return null;
    }
}
