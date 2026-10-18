package com.edusync.service;

import com.edusync.dto.AppDto;
import com.edusync.dto.DriveFileDto;
import com.edusync.dto.IndustryDto.*;
import com.edusync.dto.NotificationDto;
import com.edusync.exception.BadRequestException;
import com.edusync.exception.ResourceNotFoundException;
import com.edusync.util.FileUploadValidator;
import com.edusync.model.*;
import com.edusync.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndustryService {

    private final CompanyRepository companyRepo;
    private final IndustryMouRepository mouRepo;
    private final InternshipRepository internshipRepo;
    private final InternshipApplicationRepository applicationRepo;
    private final IndustryVisitRepository visitRepo;
    private final GuestLectureRepository guestLectureRepo;
    private final IndustryProjectRepository industryProjectRepo;
    private final AdvisoryMemberRepository advisoryMemberRepo;
    private final AlumniContactRepository alumniContactRepo;
    private final UserRepository userRepo;
    private final IndustryActivityLogRepository activityLogRepo;
    private final NotificationService notificationService;
    private final GoogleDriveService googleDriveService;
    private final PdfService pdfService;

    @org.springframework.beans.factory.annotation.Value("${edusync.industry.visits.large-group-threshold:50}")
    private int largeGroupThreshold = 50;

    public void setLargeGroupThreshold(int threshold) {
        this.largeGroupThreshold = threshold;
    }

    // =========================================================================
    // 1. KPI OVERVIEW
    // =========================================================================

    @Transactional(readOnly = true)
    public IndustryOverviewDto getOverview() {
        LocalDate today = LocalDate.now();
        LocalDate in60Days = today.plusDays(60);

        long activeMous = mouRepo.countByStatus(IndustryMou.MouStatus.ACTIVE);

        // MoUs expiring within 60 days: either marked EXPIRING or ACTIVE with validTo <= today + 60 days
        List<IndustryMou> allMous = mouRepo.findAll();
        long expiringMous = allMous.stream()
                .filter(m -> m.getStatus() == IndustryMou.MouStatus.EXPIRING ||
                        (m.getStatus() == IndustryMou.MouStatus.ACTIVE && m.getValidTo() != null && !m.getValidTo().isAfter(in60Days)))
                .count();

        // Companies by PartnershipStage
        List<Company> companies = companyRepo.findAll();
        Map<String, Long> stageCounts = new LinkedHashMap<>();
        for (Company.PartnershipStage stage : Company.PartnershipStage.values()) {
            stageCounts.put(stage.name(), 0L);
        }
        for (Company c : companies) {
            Company.PartnershipStage stage = c.getPartnershipStage() != null ? c.getPartnershipStage() : Company.PartnershipStage.LEAD;
            stageCounts.put(stage.name(), stageCounts.getOrDefault(stage.name(), 0L) + 1);
        }

        long openInternships = internshipRepo.countByStatus(Internship.InternshipStatus.OPEN);
        long totalApplications = applicationRepo.count();

        long interningStudents = applicationRepo.findByStatus(InternshipApplication.ApplicationStatus.JOINED).size();

        List<IndustryActivityLogDto> recentActivities = activityLogRepo.findTop50ByOrderByTimestampDesc().stream()
                .map(this::toActivityLogDto)
                .toList();

        List<IndustryMouDto> expiringMousList = allMous.stream()
                .filter(m -> m.getStatus() == IndustryMou.MouStatus.EXPIRING ||
                        (m.getStatus() == IndustryMou.MouStatus.ACTIVE && m.getValidTo() != null && !m.getValidTo().isAfter(in60Days)))
                .map(this::toMouDto)
                .toList();

        List<IndustryVisitDto> pendingVisitsList = visitRepo.findByStage(IndustryVisit.VisitStage.ICH_REVIEW).stream()
                .map(this::toVisitDto)
                .toList();

        return IndustryOverviewDto.builder()
                .activeMoUs(activeMous)
                .mousExpiringWithin60Days(expiringMous)
                .companiesByPartnershipStage(stageCounts)
                .openInternships(openInternships)
                .applications(totalApplications)
                .studentsCurrentlyInterning(interningStudents)
                .totalCompanies(companies.size())
                .expiringMous(expiringMousList)
                .pendingVisits(pendingVisitsList)
                .recentActivities(recentActivities)
                .build();
    }

    // =========================================================================
    // 2. COMPANIES MANAGEMENT
    // =========================================================================

    @Transactional(readOnly = true)
    public List<CompanyDto> getCompanies(String search, Company.PartnershipStage stage, Boolean placementReady) {
        List<Company> list = companyRepo.findAllByOrderByNameAsc();
        return list.stream()
                .filter(c -> search == null || search.trim().isEmpty() ||
                        c.getName().toLowerCase().contains(search.trim().toLowerCase()) ||
                        (c.getSector() != null && c.getSector().toLowerCase().contains(search.trim().toLowerCase())))
                .filter(c -> stage == null || c.getPartnershipStage() == stage)
                .filter(c -> placementReady == null || Objects.equals(c.getPlacementReady(), placementReady))
                .map(this::toCompanyDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CompanyDto getCompanyById(Long id) {
        Company company = findCompany(id);
        return toCompanyDto(company);
    }

    @Transactional
    public CompanyDto createCompany(CompanyRequest req, String actor) {
        if (req.getName() == null || req.getName().trim().isEmpty()) {
            throw new BadRequestException("Company name cannot be blank.");
        }
        if (companyRepo.findByNameIgnoreCase(req.getName().trim()).isPresent()) {
            throw new BadRequestException("A company with this name already exists: " + req.getName().trim());
        }

        Company company = Company.builder()
                .name(req.getName().trim())
                .logo(req.getLogo())
                .location(req.getLocation())
                .description(req.getDescription())
                .sector(req.getSector())
                .website(req.getWebsite())
                .hrName(req.getHrName())
                .hrEmail(req.getHrEmail())
                .hrPhone(req.getHrPhone())
                .partnershipStage(req.getPartnershipStage() != null ? req.getPartnershipStage() : Company.PartnershipStage.LEAD)
                .tier(req.getTier())
                .placementReady(Boolean.TRUE.equals(req.getPlacementReady()))
                .build();

        Company saved = companyRepo.save(company);
        logActivity(actor, "CREATE_COMPANY", "COMPANY", saved.getId(),
                "Registered company '" + saved.getName() + "' at stage " + saved.getPartnershipStage());

        return toCompanyDto(saved);
    }

    @Transactional
    public CompanyDto updateCompany(Long id, CompanyRequest req, String actor) {
        Company company = findCompany(id);

        if (req.getName() != null && !req.getName().trim().isEmpty()) {
            String newName = req.getName().trim();
            companyRepo.findByNameIgnoreCase(newName).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new BadRequestException("Another company already exists with name: " + newName);
                }
            });
            company.setName(newName);
        }

        if (req.getLogo() != null) company.setLogo(req.getLogo());
        if (req.getLocation() != null) company.setLocation(req.getLocation());
        if (req.getDescription() != null) company.setDescription(req.getDescription());
        if (req.getSector() != null) company.setSector(req.getSector());
        if (req.getWebsite() != null) company.setWebsite(req.getWebsite());
        if (req.getHrName() != null) company.setHrName(req.getHrName());
        if (req.getHrEmail() != null) company.setHrEmail(req.getHrEmail());
        if (req.getHrPhone() != null) company.setHrPhone(req.getHrPhone());
        if (req.getPartnershipStage() != null) company.setPartnershipStage(req.getPartnershipStage());
        if (req.getTier() != null) company.setTier(req.getTier());
        if (req.getPlacementReady() != null) company.setPlacementReady(req.getPlacementReady());

        Company updated = companyRepo.save(company);
        logActivity(actor, "UPDATE_COMPANY", "COMPANY", updated.getId(),
                "Updated profile for company '" + updated.getName() + "'");

        return toCompanyDto(updated);
    }

    @Transactional
    public CompanyDto updateCompanyStage(Long id, CompanyStageUpdateRequest req, String actor) {
        Company company = findCompany(id);
        Company.PartnershipStage oldStage = company.getPartnershipStage();
        company.setPartnershipStage(req.getPartnershipStage());

        if (req.getPlacementReady() != null) {
            company.setPlacementReady(req.getPlacementReady());
        }

        Company updated = companyRepo.save(company);
        logActivity(actor, "STAGE_TRANSITION", "COMPANY", updated.getId(),
                "Changed stage from " + oldStage + " to " + req.getPartnershipStage() +
                        (req.getPlacementReady() != null ? " (PlacementReady: " + req.getPlacementReady() + ")" : ""));

        return toCompanyDto(updated);
    }

    // =========================================================================
    // 3. MOUs MANAGEMENT & RENEWALS
    // =========================================================================

    @Transactional(readOnly = true)
    public List<IndustryMouDto> getMous(IndustryMou.MouStatus status, Long companyId) {
        List<IndustryMou> list = mouRepo.findAllByOrderByCreatedAtDesc();
        return list.stream()
                .filter(m -> status == null || m.getStatus() == status)
                .filter(m -> companyId == null || (m.getCompany() != null && m.getCompany().getId().equals(companyId)))
                .map(this::toMouDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public IndustryMouDto getMouById(Long id) {
        IndustryMou mou = findMou(id);
        return toMouDto(mou);
    }

    @Transactional
    public IndustryMouDto createMou(IndustryMouRequest req, String actor) {
        Company company = findCompany(req.getCompanyId());

        if (req.getTitle() == null || req.getTitle().trim().isEmpty()) {
            throw new BadRequestException("MoU title is required.");
        }
        if (req.getValidFrom() != null && req.getValidTo() != null && req.getValidTo().isBefore(req.getValidFrom())) {
            throw new BadRequestException("MoU validTo date cannot be earlier than validFrom date.");
        }

        IndustryMou.MouStatus initialStatus = req.getStatus() != null ? req.getStatus() : IndustryMou.MouStatus.DRAFT;

        IndustryMou mou = IndustryMou.builder()
                .company(company)
                .title(req.getTitle().trim())
                .signedDate(req.getSignedDate())
                .validFrom(req.getValidFrom())
                .validTo(req.getValidTo())
                .scope(req.getScope())
                .status(initialStatus)
                .driveFileId(req.getDriveFileId())
                .departments(req.getDepartments())
                .documentUrl(req.getDocumentUrl())
                .remarks(req.getRemarks())
                .build();

        IndustryMou saved = mouRepo.save(mou);

        // If newly active MoU, promote company partnershipStage if currently below MOU_SIGNED
        if (saved.getStatus() == IndustryMou.MouStatus.ACTIVE &&
                (company.getPartnershipStage() == Company.PartnershipStage.LEAD ||
                 company.getPartnershipStage() == Company.PartnershipStage.CONTACTED ||
                 company.getPartnershipStage() == Company.PartnershipStage.NEGOTIATING)) {
            company.setPartnershipStage(Company.PartnershipStage.MOU_SIGNED);
            companyRepo.save(company);
        }

        logActivity(actor, "CREATE_MOU", "MOU", saved.getId(),
                "Created MoU '" + saved.getTitle() + "' with " + company.getName() + " (Status: " + saved.getStatus() + ")");

        return toMouDto(saved);
    }

    @Transactional
    public IndustryMouDto updateMou(Long id, IndustryMouRequest req, String actor) {
        IndustryMou mou = findMou(id);

        if (req.getCompanyId() != null && !req.getCompanyId().equals(mou.getCompany().getId())) {
            Company company = findCompany(req.getCompanyId());
            mou.setCompany(company);
        }
        if (req.getTitle() != null && !req.getTitle().trim().isEmpty()) {
            mou.setTitle(req.getTitle().trim());
        }
        if (req.getSignedDate() != null) mou.setSignedDate(req.getSignedDate());
        if (req.getValidFrom() != null) mou.setValidFrom(req.getValidFrom());
        if (req.getValidTo() != null) {
            if (mou.getValidFrom() != null && req.getValidTo().isBefore(mou.getValidFrom())) {
                throw new BadRequestException("validTo cannot be before validFrom.");
            }
            mou.setValidTo(req.getValidTo());
        }
        if (req.getScope() != null) mou.setScope(req.getScope());
        if (req.getStatus() != null) mou.setStatus(req.getStatus());
        if (req.getDriveFileId() != null) mou.setDriveFileId(req.getDriveFileId());
        if (req.getDepartments() != null) mou.setDepartments(req.getDepartments());
        if (req.getDocumentUrl() != null) mou.setDocumentUrl(req.getDocumentUrl());
        if (req.getRemarks() != null) mou.setRemarks(req.getRemarks());

        IndustryMou updated = mouRepo.save(mou);
        logActivity(actor, "UPDATE_MOU", "MOU", updated.getId(),
                "Updated details for MoU '" + updated.getTitle() + "'");

        return toMouDto(updated);
    }

    @Transactional
    public IndustryMouDto renewMou(Long id, MouRenewalRequest req, String actor) {
        IndustryMou mou = findMou(id);

        if (req.getValidTo().isBefore(req.getValidFrom())) {
            throw new BadRequestException("Renewal validTo date cannot be earlier than validFrom.");
        }

        mou.setValidFrom(req.getValidFrom());
        mou.setValidTo(req.getValidTo());
        mou.setStatus(IndustryMou.MouStatus.ACTIVE);
        if (req.getScope() != null && !req.getScope().trim().isEmpty()) {
            mou.setScope(req.getScope());
        }
        if (req.getRemarks() != null) {
            String existingRemarks = mou.getRemarks() != null ? mou.getRemarks() + "\n" : "";
            mou.setRemarks(existingRemarks + "[Renewed " + LocalDate.now() + "]: " + req.getRemarks());
        }

        IndustryMou updated = mouRepo.save(mou);
        logActivity(actor, "RENEW_MOU", "MOU", updated.getId(),
                "Renewed MoU until " + req.getValidTo());

        return toMouDto(updated);
    }

    @Transactional(readOnly = true)
    public List<IndustryMouDto> getExpiringMous() {
        LocalDate today = LocalDate.now();
        LocalDate in60Days = today.plusDays(60);

        return mouRepo.findAll().stream()
                .filter(m -> m.getStatus() == IndustryMou.MouStatus.EXPIRING ||
                        (m.getStatus() == IndustryMou.MouStatus.ACTIVE && m.getValidTo() != null && !m.getValidTo().isAfter(in60Days)))
                .sorted(Comparator.comparing(IndustryMou::getValidTo, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toMouDto)
                .toList();
    }

    @Transactional
    public IndustryMouDto uploadMouDocument(Long mouId, MultipartFile file, String actor) {
        IndustryMou mou = findMou(mouId);

        // Strict validation: pdf, png, jpg, jpeg only, magic bytes verified, size capped at 10MB
        FileUploadValidator.ValidatedFile validated = FileUploadValidator.validateUpload(file, true);

        try {
            String originalName = validated.getSanitizedFilename();
            String cleanCompanyName = mou.getCompany().getName().replaceAll("[^a-zA-Z0-9_-]", "_");
            String gDriveFileName = "MOU_" + mou.getId() + "_" + cleanCompanyName + "_" + System.currentTimeMillis() + "_" + originalName;

            DriveFileDto uploadResult = googleDriveService.uploadFile(
                    "Industry-MoUs",
                    gDriveFileName,
                    validated.getDerivedMimeType(),
                    validated.getBytes()
            );

            mou.setDriveFileId(uploadResult.getFileId());
            mou.setDocumentUrl(uploadResult.getWebViewLink());

            IndustryMou updated = mouRepo.save(mou);
            logActivity(actor, "UPLOAD_MOU_DOCUMENT", "MOU", updated.getId(),
                    "Uploaded signed MoU copy (" + uploadResult.getFileId() + ")");

            return toMouDto(updated);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload MoU document: {}", e.getMessage(), e);
            throw new BadRequestException("MoU upload failed: " + e.getMessage());
        }
    }

    // =========================================================================
    // 4. INTERNSHIPS & APPLICATIONS
    // =========================================================================

    @Transactional(readOnly = true)
    public List<InternshipDto> getInternships(Internship.InternshipStatus status, Long companyId, String department) {
        List<Internship> list = internshipRepo.findAllByOrderByCreatedAtDesc();
        return list.stream()
                .filter(i -> status == null || i.getStatus() == status)
                .filter(i -> companyId == null || (i.getCompany() != null && i.getCompany().getId().equals(companyId)))
                .filter(i -> department == null || department.trim().isEmpty() || isDepartmentEligible(i.getDepartments(), department))
                .map(this::toInternshipDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public InternshipDto getInternshipById(Long id) {
        Internship internship = findInternship(id);
        return toInternshipDto(internship);
    }

    /**
     * Creates an internship posting.
     * Rule: Only companies with an ACTIVE MoU can have internships posted.
     */
    @Transactional
    public InternshipDto createInternship(InternshipRequest req, String actor) {
        Company company = findCompany(req.getCompanyId());

        // Validate ACTIVE MoU
        boolean hasActiveMou = mouRepo.findByCompanyId(company.getId()).stream()
                .anyMatch(m -> m.getStatus() == IndustryMou.MouStatus.ACTIVE
                        && (m.getValidTo() == null || !m.getValidTo().isBefore(LocalDate.now())));

        if (!hasActiveMou) {
            throw new BadRequestException("Cannot post internship: Company '" + company.getName() + "' does not have an ACTIVE MoU.");
        }

        if (req.getTitle() == null || req.getTitle().trim().isEmpty()) {
            throw new BadRequestException("Internship title is required.");
        }

        Internship.InternshipStatus initialStatus = req.getStatus() != null ? req.getStatus() : Internship.InternshipStatus.OPEN;

        Internship internship = Internship.builder()
                .company(company)
                .title(req.getTitle().trim())
                .description(req.getDescription())
                .requiredSkills(req.getRequiredSkills())
                .duration(req.getDuration())
                .stipend(req.getStipend())
                .mode(req.getMode())
                .departments(req.getDepartments())
                .seats(req.getSeats())
                .deadline(req.getDeadline())
                .status(initialStatus)
                .location(req.getLocation())
                .build();

        Internship saved = internshipRepo.save(internship);
        logActivity(actor, "CREATE_INTERNSHIP", "INTERNSHIP", saved.getId(),
                "Posted internship '" + saved.getTitle() + "' with " + company.getName() + " (" + saved.getSeats() + " seats)");

        return toInternshipDto(saved);
    }

    @Transactional
    public InternshipDto updateInternship(Long id, InternshipRequest req, String actor) {
        Internship internship = findInternship(id);

        if (req.getCompanyId() != null && !req.getCompanyId().equals(internship.getCompany().getId())) {
            Company company = findCompany(req.getCompanyId());
            boolean hasActiveMou = mouRepo.findByCompanyId(company.getId()).stream()
                    .anyMatch(m -> m.getStatus() == IndustryMou.MouStatus.ACTIVE
                            && (m.getValidTo() == null || !m.getValidTo().isBefore(LocalDate.now())));
            if (!hasActiveMou) {
                throw new BadRequestException("Company does not have an active MoU.");
            }
            internship.setCompany(company);
        }

        if (req.getTitle() != null && !req.getTitle().trim().isEmpty()) internship.setTitle(req.getTitle().trim());
        if (req.getDescription() != null) internship.setDescription(req.getDescription());
        if (req.getRequiredSkills() != null) internship.setRequiredSkills(req.getRequiredSkills());
        if (req.getDuration() != null) internship.setDuration(req.getDuration());
        if (req.getStipend() != null) internship.setStipend(req.getStipend());
        if (req.getMode() != null) internship.setMode(req.getMode());
        if (req.getDepartments() != null) internship.setDepartments(req.getDepartments());
        if (req.getSeats() != null) internship.setSeats(req.getSeats());
        if (req.getDeadline() != null) internship.setDeadline(req.getDeadline());
        if (req.getStatus() != null) internship.setStatus(req.getStatus());
        if (req.getLocation() != null) internship.setLocation(req.getLocation());

        Internship updated = internshipRepo.save(internship);
        logActivity(actor, "UPDATE_INTERNSHIP", "INTERNSHIP", updated.getId(),
                "Updated internship details for '" + updated.getTitle() + "'");

        return toInternshipDto(updated);
    }

    /**
     * Student view: returns only OPEN internships before the deadline matching student's department.
     * Response does NOT include company HR details.
     */
    @Transactional(readOnly = true)
    public List<InternshipDto> getOpenInternshipsForStudent(String studentDepartment) {
        LocalDate today = LocalDate.now();
        List<Internship> openList = internshipRepo.findByStatus(Internship.InternshipStatus.OPEN);

        return openList.stream()
                .filter(i -> i.getDeadline() == null || !i.getDeadline().isBefore(today))
                .filter(i -> isDepartmentEligible(i.getDepartments(), studentDepartment))
                .sorted(Comparator.comparing(Internship::getDeadline, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toInternshipDto)
                .toList();
    }

    /**
     * Student applies for an internship:
     * - Enforces deadline
     * - Blocks duplicate applications
     * - Enforces seat limits
     * - Dispatches FCM notification to the student
     */
    @Transactional
    public InternshipApplicationDto applyForInternship(Long internshipId, Long studentId, InternshipApplicationRequest req, String actor) {
        Internship internship = findInternship(internshipId);
        User student = userRepo.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        LocalDate today = LocalDate.now();

        // 1. Check Internship Status
        if (internship.getStatus() != Internship.InternshipStatus.OPEN) {
            throw new BadRequestException("This internship is not open for applications.");
        }

        // 2. Deadline check
        if (internship.getDeadline() != null && internship.getDeadline().isBefore(today)) {
            throw new BadRequestException("The deadline for this internship has passed.");
        }

        // 3. Duplicate check
        if (applicationRepo.existsByInternshipIdAndStudentId(internshipId, studentId)) {
            throw new BadRequestException("You have already applied for this internship.");
        }

        // 4. Seat limit rule
        if (internship.getSeats() != null) {
            long currentCount = applicationRepo.countByInternshipId(internshipId);
            if (currentCount >= internship.getSeats()) {
                throw new BadRequestException("Application limit reached: no seats remaining for this internship.");
            }
        }

        // 5. Department eligibility rule
        if (!isDepartmentEligible(internship.getDepartments(), student.getDepartment())) {
            throw new BadRequestException("This internship is not open to students of the " + student.getDepartment() + " department.");
        }

        InternshipApplication application = InternshipApplication.builder()
                .internship(internship)
                .student(student)
                .resumeUrl(req != null ? req.getResumeUrl() : null)
                .status(InternshipApplication.ApplicationStatus.APPLIED)
                .feedback(req != null ? req.getFeedback() : null)
                .appliedAt(LocalDateTime.now())
                .build();

        InternshipApplication saved = applicationRepo.save(application);

        logActivity(actor, "APPLY_INTERNSHIP", "APPLICATION", saved.getId(),
                "Student " + student.getName() + " applied to '" + internship.getTitle() + "' at " + internship.getCompany().getName());

        // Notify student via FCM/SSE
        sendStudentNotification(student.getId(),
                "✅ Internship Application Submitted",
                "Your application for '" + internship.getTitle() + "' at " + internship.getCompany().getName() + " has been received.",
                "#page-internships");

        return toApplicationDto(saved);
    }

    @Transactional(readOnly = true)
    public List<InternshipApplicationDto> getInternshipApplications(Long internshipId) {
        List<InternshipApplication> list = applicationRepo.findByInternshipIdOrderByAppliedAtDesc(internshipId);
        return list.stream()
                .map(this::toApplicationDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InternshipApplicationDto> getStudentApplications(Long studentId) {
        List<InternshipApplication> list = applicationRepo.findByStudentIdOrderByAppliedAtDesc(studentId);
        return list.stream()
                .map(this::toApplicationDto)
                .toList();
    }

    /**
     * Updates an application status and notifies student via FCM on every change.
     */
    @Transactional
    public InternshipApplicationDto updateApplicationStatus(Long applicationId, InternshipApplicationStatusRequest req, String actor) {
        InternshipApplication app = findApplication(applicationId);
        InternshipApplication.ApplicationStatus oldStatus = app.getStatus();
        app.setStatus(req.getStatus());

        if (req.getFeedback() != null) {
            app.setFeedback(req.getFeedback());
        }
        if (req.getCertificateUrl() != null) {
            app.setCertificateUrl(req.getCertificateUrl());
        }

        InternshipApplication updated = applicationRepo.save(app);

        logActivity(actor, "APPLICATION_STATUS_UPDATE", "APPLICATION", updated.getId(),
                "Updated status for student " + updated.getStudent().getName() + " from " + oldStatus + " to " + updated.getStatus());

        // Notify student via FCM
        String statusMsg = "Your application status for '" + updated.getInternship().getTitle() +
                "' at " + updated.getInternship().getCompany().getName() + " is now: " + updated.getStatus();
        if (updated.getFeedback() != null && !updated.getFeedback().trim().isEmpty()) {
            statusMsg += ". Remarks: " + updated.getFeedback();
        }

        sendStudentNotification(updated.getStudent().getId(),
                "📢 Internship Status Update: " + updated.getStatus(),
                statusMsg,
                "#page-internships");

        return toApplicationDto(updated);
    }

    /**
     * Uploads completion certificate to Google Drive and sets application status to COMPLETED.
     */
    @Transactional
    public InternshipApplicationDto uploadApplicationCertificate(Long applicationId, MultipartFile file, String actor) {
        InternshipApplication app = findApplication(applicationId);

        // Strict validation: pdf, png, jpg, jpeg only, magic bytes verified, size capped at 10MB
        FileUploadValidator.ValidatedFile validated = FileUploadValidator.validateUpload(file, true);

        try {
            String originalName = validated.getSanitizedFilename();
            String studentName = app.getStudent().getName().replaceAll("[^a-zA-Z0-9_-]", "_");
            String gDriveFileName = "CERT_" + app.getId() + "_" + studentName + "_" + System.currentTimeMillis() + "_" + originalName;

            DriveFileDto uploadResult = googleDriveService.uploadFile(
                    "Internship-Certificates",
                    gDriveFileName,
                    validated.getDerivedMimeType(),
                    validated.getBytes()
            );

            app.setCertificateUrl(uploadResult.getWebViewLink());
            app.setStatus(InternshipApplication.ApplicationStatus.COMPLETED);

            InternshipApplication updated = applicationRepo.save(app);

            logActivity(actor, "UPLOAD_CERTIFICATE", "APPLICATION", updated.getId(),
                    "Uploaded completion certificate for student " + updated.getStudent().getName());

            // Notify student via FCM
            sendStudentNotification(updated.getStudent().getId(),
                    "🎓 Internship Certificate Issued!",
                    "Your completion certificate for '" + updated.getInternship().getTitle() + "' is now available.",
                    "#page-internships");

            return toApplicationDto(updated);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload internship certificate to Google Drive: {}", e.getMessage(), e);
            throw new BadRequestException("Certificate upload failed: " + e.getMessage());
        }
    }

    // =========================================================================
    // 5. DAILY SCHEDULED JOB: EXPIRATION CHECK & NOTIFICATIONS
    // =========================================================================

    @Scheduled(cron = "${edusync.industry.expiry-cron:0 0 2 * * ?}")
    @Transactional
    public int processDailyMouExpirations() {
        return processDailyMouExpirationsForDate(LocalDate.now());
    }

    @Transactional
    public int processDailyMouExpirationsForDate(LocalDate currentDate) {
        LocalDate in60Days = currentDate.plusDays(60);
        List<IndustryMou> allMous = mouRepo.findAll();
        int transitionedCount = 0;

        List<IndustryMou> newlyExpired = new ArrayList<>();
        List<IndustryMou> newlyExpiring = new ArrayList<>();

        for (IndustryMou mou : allMous) {
            if (mou.getValidTo() == null) continue;

            // 1. Active or Expiring MoUs past their validity -> EXPIRED
            if ((mou.getStatus() == IndustryMou.MouStatus.ACTIVE || mou.getStatus() == IndustryMou.MouStatus.EXPIRING)
                    && (mou.getValidTo().isBefore(currentDate) || mou.getValidTo().isEqual(currentDate))) {
                mou.setStatus(IndustryMou.MouStatus.EXPIRED);
                mouRepo.save(mou);
                newlyExpired.add(mou);
                transitionedCount++;

                logActivity("SYSTEM_SCHEDULER", "MOU_EXPIRED", "MOU", mou.getId(),
                        "MoU '" + mou.getTitle() + "' with " + mou.getCompany().getName() + " reached expiry on " + mou.getValidTo());
            }
            // 2. Active MoUs within 60 days threshold -> EXPIRING
            else if (mou.getStatus() == IndustryMou.MouStatus.ACTIVE
                    && !mou.getValidTo().isAfter(in60Days)
                    && mou.getValidTo().isAfter(currentDate)) {
                mou.setStatus(IndustryMou.MouStatus.EXPIRING);
                mouRepo.save(mou);
                newlyExpiring.add(mou);
                transitionedCount++;

                logActivity("SYSTEM_SCHEDULER", "MOU_EXPIRING", "MOU", mou.getId(),
                        "MoU '" + mou.getTitle() + "' with " + mou.getCompany().getName() + " expires soon on " + mou.getValidTo());
            }
        }

        // Notify Industrial Connect Heads if changes occurred
        if (!newlyExpired.isEmpty() || !newlyExpiring.isEmpty()) {
            dispatchExpiryNotification(newlyExpiring, newlyExpired);
        }

        return transitionedCount;
    }

    private void dispatchExpiryNotification(List<IndustryMou> expiring, List<IndustryMou> expired) {
        StringBuilder msg = new StringBuilder();
        if (!expiring.isEmpty()) {
            msg.append(expiring.size()).append(" MoU(s) expiring within 60 days. ");
        }
        if (!expired.isEmpty()) {
            msg.append(expired.size()).append(" MoU(s) reached expiration today.");
        }

        NotificationDto notif = NotificationDto.builder()
                .id(UUID.randomUUID().toString())
                .type("MOU_EXPIRY_ALERT")
                .title("⚠️ Industry MoU Status Alert")
                .message(msg.toString().trim())
                .link("#page-industry")
                .actorName("EduSync Industry Engine")
                .targetRole(User.Role.INDUSTRIAL_CONNECT_HEAD.name())
                .timestamp(LocalDateTime.now())
                .build();

        try {
            notificationService.sendToRole(User.Role.INDUSTRIAL_CONNECT_HEAD, notif);
            notificationService.sendToRole(User.Role.ADMIN, notif);
        } catch (Exception e) {
            log.warn("Notice: Could not dispatch FCM/SSE notification for MoU expiry: {}", e.getMessage());
        }
    }

    private void sendStudentNotification(Long studentId, String title, String message, String link) {
        NotificationDto notif = NotificationDto.builder()
                .id(UUID.randomUUID().toString())
                .type("INTERNSHIP_NOTIFICATION")
                .title(title)
                .message(message)
                .link(link)
                .actorName("Industry Relations")
                .targetRole(User.Role.STUDENT.name())
                .timestamp(LocalDateTime.now())
                .build();
        try {
            notificationService.sendToUser(studentId, notif);
        } catch (Exception e) {
            log.warn("Notice: Could not dispatch notification to student #{}: {}", studentId, e.getMessage());
        }
    }

    // =========================================================================
    // 6. INDUSTRIAL VISITS APPROVAL FLOW
    // =========================================================================

    @Transactional
    public IndustryVisitDto proposeVisit(IndustryVisitRequest request, Long proposerId, String actor) {
        if (request.getCompanyId() == null) {
            throw new BadRequestException("Company ID is required");
        }
        if (request.getDate() == null) {
            throw new BadRequestException("Visit date is required");
        }
        Company company = findCompany(request.getCompanyId());

        User proposer = null;
        if (proposerId != null) {
            proposer = userRepo.findById(proposerId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", proposerId));
        } else if (actor != null) {
            proposer = userRepo.findByEmailIgnoreCase(actor).orElse(null);
        }
        if (proposer == null) {
            throw new BadRequestException("Could not identify proposing user");
        }

        String dept = request.getDepartment();
        if (dept == null || dept.trim().isEmpty()) {
            dept = proposer.getDepartment();
        }
        if (dept == null || dept.trim().isEmpty()) {
            dept = "General";
        }

        boolean overnight = Boolean.TRUE.equals(request.getOvernight());
        boolean outsideKerala = Boolean.TRUE.equals(request.getOutsideKerala());
        boolean largeGroup = request.getStudentCount() != null && request.getStudentCount() > largeGroupThreshold;
        boolean requiresPrincipal = overnight || outsideKerala || largeGroup;

        IndustryVisit visit = IndustryVisit.builder()
                .company(company)
                .proposedBy(proposer)
                .department(dept)
                .date(request.getDate())
                .studentCount(request.getStudentCount())
                .purpose(request.getPurpose())
                .location(request.getLocation() != null ? request.getLocation() : company.getLocation())
                .overnight(overnight)
                .outsideKerala(outsideKerala)
                .stage(IndustryVisit.VisitStage.ICH_REVIEW)
                .status(IndustryVisit.VisitStatus.PENDING)
                .ichStatus(IndustryVisit.ApprovalStatus.PENDING)
                .hodStatus(IndustryVisit.ApprovalStatus.PENDING)
                .principalStatus(requiresPrincipal ? IndustryVisit.ApprovalStatus.PENDING : IndustryVisit.ApprovalStatus.NOT_REQUIRED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        IndustryVisit saved = visitRepo.save(visit);

        // Activity log entry
        logActivity(
                actor != null ? actor : proposer.getEmail(),
                "VISIT_PROPOSED",
                "IndustryVisit",
                saved.getId(),
                "Industrial visit to " + company.getName() + " proposed by " + proposer.getName() + " (" + dept + ") for " + saved.getDate()
        );

        // FCM notification to proposer
        try {
            notificationService.sendToUser(
                    proposer.getId(),
                    NotificationDto.builder()
                            .type("VISIT_SUBMITTED")
                            .title("Industrial Visit Proposal Submitted")
                            .message("Your proposal for visit to " + company.getName() + " on " + saved.getDate() + " is now in ICH review.")
                            .actorName(proposer.getName())
                            .department(saved.getDepartment())
                            .link("#page-industry")
                            .timestamp(LocalDateTime.now())
                            .build()
            );
        } catch (Exception e) {
            log.warn("Could not send FCM notification for visit proposal: {}", e.getMessage());
        }

        return toVisitDto(saved);
    }

    @Transactional
    public IndustryVisitDto ichVisitAction(Long visitId, VisitActionRequest request, String actor) {
        IndustryVisit visit = findVisit(visitId);

        if (request.getStatus() == null) {
            throw new BadRequestException("Action status is required (APPROVED or REJECTED)");
        }

        visit.setIchStatus(request.getStatus());
        visit.setIchRemark(request.getRemark());
        visit.setIchApprovedBy(actor);
        visit.setIchApprovedAt(LocalDateTime.now());

        if (request.getStatus() == IndustryVisit.ApprovalStatus.REJECTED) {
            visit.setStatus(IndustryVisit.VisitStatus.REJECTED);
            visit.setStage(IndustryVisit.VisitStage.COMPLETED);
        } else {
            visit.setStage(IndustryVisit.VisitStage.HOD_REVIEW);
        }

        IndustryVisit saved = visitRepo.save(visit);

        // Activity log entry
        logActivity(
                actor,
                request.getStatus() == IndustryVisit.ApprovalStatus.APPROVED ? "VISIT_ICH_APPROVED" : "VISIT_ICH_REJECTED",
                "IndustryVisit",
                saved.getId(),
                "ICH " + request.getStatus().name().toLowerCase() + " visit proposal #" + saved.getId() + " (" + saved.getCompany().getName() + ")"
        );

        // FCM notification to proposer
        if (saved.getProposedBy() != null) {
            String title = request.getStatus() == IndustryVisit.ApprovalStatus.APPROVED
                    ? "Industrial Visit Approved by ICH"
                    : "Industrial Visit Rejected by ICH";
            String msg = "ICH has " + request.getStatus().name().toLowerCase() + " your visit proposal to " + saved.getCompany().getName()
                    + (request.getRemark() != null && !request.getRemark().isBlank() ? ": " + request.getRemark() : "");

            try {
                notificationService.sendToUser(
                        saved.getProposedBy().getId(),
                        NotificationDto.builder()
                                .type("VISIT_UPDATE")
                                .title(title)
                                .message(msg)
                                .actorName(actor)
                                .department(saved.getDepartment())
                                .link("#page-industry")
                                .timestamp(LocalDateTime.now())
                                .build()
                );
            } catch (Exception e) {
                log.warn("Could not send FCM notification for ICH action: {}", e.getMessage());
            }
        }

        return toVisitDto(saved);
    }

    @Transactional
    public IndustryVisitDto hodVisitAction(Long visitId, Long hodUserId, VisitActionRequest request, String actor) {
        IndustryVisit visit = findVisit(visitId);

        if (request.getStatus() == null) {
            throw new BadRequestException("Action status is required (APPROVED or REJECTED)");
        }

        visit.setHodStatus(request.getStatus());
        visit.setHodRemark(request.getRemark());
        visit.setHodApprovedBy(actor);
        visit.setHodApprovedAt(LocalDateTime.now());

        if (request.getStatus() == IndustryVisit.ApprovalStatus.REJECTED) {
            visit.setStatus(IndustryVisit.VisitStatus.REJECTED);
            visit.setStage(IndustryVisit.VisitStage.COMPLETED);
        } else {
            boolean requiresPrincipal = Boolean.TRUE.equals(visit.getOvernight())
                    || Boolean.TRUE.equals(visit.getOutsideKerala())
                    || (visit.getStudentCount() != null && visit.getStudentCount() > largeGroupThreshold);

            if (requiresPrincipal) {
                visit.setStage(IndustryVisit.VisitStage.PRINCIPAL_REVIEW);
                visit.setPrincipalStatus(IndustryVisit.ApprovalStatus.PENDING);
            } else {
                visit.setPrincipalStatus(IndustryVisit.ApprovalStatus.NOT_REQUIRED);
                visit.setStatus(IndustryVisit.VisitStatus.APPROVED);
                visit.setStage(IndustryVisit.VisitStage.COMPLETED);
                visit.setPdfPath("/api/industry/visits/" + visit.getId() + "/sanction-letter");
            }
        }

        IndustryVisit saved = visitRepo.save(visit);

        // Activity log entry
        logActivity(
                actor,
                request.getStatus() == IndustryVisit.ApprovalStatus.APPROVED ? "VISIT_HOD_APPROVED" : "VISIT_HOD_REJECTED",
                "IndustryVisit",
                saved.getId(),
                "HOD " + request.getStatus().name().toLowerCase() + " visit proposal #" + saved.getId() + " (stage=" + saved.getStage() + ")"
        );

        // FCM notification to proposer
        if (saved.getProposedBy() != null) {
            String title;
            String msg;
            if (request.getStatus() == IndustryVisit.ApprovalStatus.REJECTED) {
                title = "Industrial Visit Rejected by HOD";
                msg = "HOD has rejected your visit proposal to " + saved.getCompany().getName()
                        + (request.getRemark() != null && !request.getRemark().isBlank() ? ": " + request.getRemark() : "");
            } else if (saved.getStage() == IndustryVisit.VisitStage.PRINCIPAL_REVIEW) {
                title = "Industrial Visit Endorsed by HOD (Principal Review Required)";
                msg = "HOD has endorsed your visit to " + saved.getCompany().getName() + ". Forwarded to Principal for sanction (overnight/outside Kerala/large group).";
            } else {
                title = "Industrial Visit Approved by HOD";
                msg = "Your industrial visit to " + saved.getCompany().getName() + " has been approved!";
            }

            try {
                notificationService.sendToUser(
                        saved.getProposedBy().getId(),
                        NotificationDto.builder()
                                .type("VISIT_UPDATE")
                                .title(title)
                                .message(msg)
                                .actorName(actor)
                                .department(saved.getDepartment())
                                .link("#page-industry")
                                .timestamp(LocalDateTime.now())
                                .build()
                );
            } catch (Exception e) {
                log.warn("Could not send FCM notification for HOD action: {}", e.getMessage());
            }
        }

        return toVisitDto(saved);
    }

    @Transactional
    public IndustryVisitDto principalVisitAction(Long visitId, VisitActionRequest request, String actor) {
        IndustryVisit visit = findVisit(visitId);

        if (request.getStatus() == null) {
            throw new BadRequestException("Action status is required (APPROVED or REJECTED)");
        }

        visit.setPrincipalStatus(request.getStatus());
        visit.setPrincipalRemark(request.getRemark());
        visit.setPrincipalApprovedAt(LocalDateTime.now());

        if (request.getStatus() == IndustryVisit.ApprovalStatus.APPROVED) {
            visit.setStatus(IndustryVisit.VisitStatus.APPROVED);
            visit.setStage(IndustryVisit.VisitStage.COMPLETED);
            visit.setPdfPath("/api/industry/visits/" + visit.getId() + "/sanction-letter");
        } else {
            visit.setStatus(IndustryVisit.VisitStatus.REJECTED);
            visit.setStage(IndustryVisit.VisitStage.COMPLETED);
        }

        IndustryVisit saved = visitRepo.save(visit);

        // Activity log entry
        logActivity(
                actor,
                request.getStatus() == IndustryVisit.ApprovalStatus.APPROVED ? "VISIT_PRINCIPAL_APPROVED" : "VISIT_PRINCIPAL_REJECTED",
                "IndustryVisit",
                saved.getId(),
                "Principal " + request.getStatus().name().toLowerCase() + " visit proposal #" + saved.getId()
        );

        // FCM notification to proposer
        if (saved.getProposedBy() != null) {
            String title = request.getStatus() == IndustryVisit.ApprovalStatus.APPROVED
                    ? "Industrial Visit Sanctioned by Principal"
                    : "Industrial Visit Rejected by Principal";
            String msg = "Principal has " + request.getStatus().name().toLowerCase() + " your industrial visit proposal to " + saved.getCompany().getName()
                    + (request.getRemark() != null && !request.getRemark().isBlank() ? ": " + request.getRemark() : "");

            try {
                notificationService.sendToUser(
                        saved.getProposedBy().getId(),
                        NotificationDto.builder()
                                .type("VISIT_UPDATE")
                                .title(title)
                                .message(msg)
                                .actorName(actor)
                                .department(saved.getDepartment())
                                .link("#page-industry")
                                .timestamp(LocalDateTime.now())
                                .build()
                );
            } catch (Exception e) {
                log.warn("Could not send FCM notification for Principal action: {}", e.getMessage());
            }
        }

        return toVisitDto(saved);
    }

    @Transactional(readOnly = true)
    public List<IndustryVisitDto> getVisits(User currentUser, String department, IndustryVisit.VisitStage stage, IndustryVisit.VisitStatus status) {
        List<IndustryVisit> visits;

        if (currentUser == null) {
            visits = visitRepo.findAllByOrderByCreatedAtDesc();
        } else {
            switch (currentUser.getRole()) {
                case FACULTY -> visits = visitRepo.findByProposedByIdOrderByCreatedAtDesc(currentUser.getId());
                case HOD -> {
                    String dept = currentUser.getDepartment();
                    if (dept != null && !dept.trim().isEmpty()) {
                        visits = visitRepo.findByDepartmentOrderByCreatedAtDesc(dept);
                    } else {
                        visits = visitRepo.findAllByOrderByCreatedAtDesc();
                    }
                }
                case INDUSTRIAL_CONNECT_HEAD, PRINCIPAL, ADMIN -> {
                    if (department != null && !department.trim().isEmpty()) {
                        visits = visitRepo.findByDepartmentOrderByCreatedAtDesc(department);
                    } else {
                        visits = visitRepo.findAllByOrderByCreatedAtDesc();
                    }
                }
                default -> visits = visitRepo.findByProposedByIdOrderByCreatedAtDesc(currentUser.getId());
            }
        }

        return visits.stream()
                .filter(v -> stage == null || v.getStage() == stage)
                .filter(v -> status == null || v.getStatus() == status)
                .map(this::toVisitDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public IndustryVisitDto getVisitById(Long id) {
        return toVisitDto(findVisit(id));
    }

    // =========================================================================
    // 7. GUEST LECTURES & SESSIONS
    // =========================================================================

    @Transactional(readOnly = true)
    public List<GuestLectureDto> getLectures(GuestLecture.LectureStatus status, String department) {
        List<GuestLecture> lectures = guestLectureRepo.findAllByOrderByDateDesc();
        return lectures.stream()
                .filter(l -> status == null || l.getStatus() == status)
                .filter(l -> department == null || department.trim().isEmpty() || matchesDept(l.getDepartments(), department))
                .map(this::toLectureDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public GuestLectureDto getLectureById(Long id) {
        return guestLectureRepo.findById(id)
                .map(this::toLectureDto)
                .orElseThrow(() -> new ResourceNotFoundException("GuestLecture", id));
    }

    @Transactional
    public GuestLectureDto createLecture(GuestLectureRequest req, String actor) {
        Company company = null;
        if (req.getCompanyId() != null) {
            company = findCompany(req.getCompanyId());
        }
        User coordinator = null;
        if (req.getCoordinatorId() != null) {
            coordinator = userRepo.findById(req.getCoordinatorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Coordinator User", req.getCoordinatorId()));
        }

        GuestLecture lecture = GuestLecture.builder()
                .speaker(req.getSpeaker())
                .designation(req.getDesignation())
                .company(company)
                .companyName(company != null ? company.getName() : req.getCompanyName())
                .topic(req.getTopic())
                .date(req.getDate())
                .time(req.getTime())
                .venue(req.getVenue())
                .departments(req.getDepartments())
                .attendance(req.getAttendance())
                .feedbackAvg(req.getFeedbackAvg())
                .coordinator(coordinator)
                .status(req.getStatus() != null ? req.getStatus() : GuestLecture.LectureStatus.UPCOMING)
                .driveFileId(req.getDriveFileId())
                .build();

        GuestLecture saved = guestLectureRepo.save(lecture);
        logActivity(actor, "CREATE_GUEST_LECTURE", "GuestLecture", saved.getId(), "Organized lecture: " + saved.getTopic() + " by " + saved.getSpeaker());
        return toLectureDto(saved);
    }

    @Transactional
    public GuestLectureDto updateLecture(Long id, GuestLectureRequest req, String actor) {
        GuestLecture lecture = guestLectureRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GuestLecture", id));

        if (req.getCompanyId() != null) {
            Company company = findCompany(req.getCompanyId());
            lecture.setCompany(company);
            lecture.setCompanyName(company.getName());
        } else if (req.getCompanyName() != null) {
            lecture.setCompanyName(req.getCompanyName());
        }

        if (req.getCoordinatorId() != null) {
            User coordinator = userRepo.findById(req.getCoordinatorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Coordinator User", req.getCoordinatorId()));
            lecture.setCoordinator(coordinator);
        }

        if (req.getSpeaker() != null) lecture.setSpeaker(req.getSpeaker());
        if (req.getDesignation() != null) lecture.setDesignation(req.getDesignation());
        if (req.getTopic() != null) lecture.setTopic(req.getTopic());
        if (req.getDate() != null) lecture.setDate(req.getDate());
        if (req.getTime() != null) lecture.setTime(req.getTime());
        if (req.getVenue() != null) lecture.setVenue(req.getVenue());
        if (req.getDepartments() != null) lecture.setDepartments(req.getDepartments());
        if (req.getAttendance() != null) lecture.setAttendance(req.getAttendance());
        if (req.getFeedbackAvg() != null) lecture.setFeedbackAvg(req.getFeedbackAvg());
        if (req.getStatus() != null) lecture.setStatus(req.getStatus());
        if (req.getDriveFileId() != null) lecture.setDriveFileId(req.getDriveFileId());

        GuestLecture saved = guestLectureRepo.save(lecture);
        logActivity(actor, "UPDATE_GUEST_LECTURE", "GuestLecture", saved.getId(), "Updated lecture: " + saved.getTopic());
        return toLectureDto(saved);
    }

    // =========================================================================
    // 8. INDUSTRY PROJECTS
    // =========================================================================

    @Transactional(readOnly = true)
    public List<IndustryProjectDto> getProjects(IndustryProject.ProjectStatus status, String department, Long companyId) {
        List<IndustryProject> projects = industryProjectRepo.findAllByOrderByCreatedAtDesc();
        return projects.stream()
                .filter(p -> status == null || p.getStatus() == status)
                .filter(p -> department == null || department.trim().isEmpty() || (p.getDepartment() != null && p.getDepartment().equalsIgnoreCase(department)))
                .filter(p -> companyId == null || (p.getCompany() != null && p.getCompany().getId().equals(companyId)))
                .map(this::toProjectDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public IndustryProjectDto getProjectById(Long id) {
        return industryProjectRepo.findById(id)
                .map(this::toProjectDto)
                .orElseThrow(() -> new ResourceNotFoundException("IndustryProject", id));
    }

    @Transactional
    public IndustryProjectDto createProject(IndustryProjectRequest req, String actor) {
        Company company = findCompany(req.getCompanyId());

        IndustryProject project = IndustryProject.builder()
                .company(company)
                .title(req.getTitle())
                .problemStatement(req.getProblemStatement())
                .department(req.getDepartment())
                .mentor(req.getMentor())
                .teamStudents(req.getTeamStudents())
                .status(req.getStatus() != null ? req.getStatus() : IndustryProject.ProjectStatus.PROPOSED)
                .outcome(req.getOutcome())
                .academicYear(req.getAcademicYear())
                .driveFileId(req.getDriveFileId())
                .build();

        IndustryProject saved = industryProjectRepo.save(project);
        logActivity(actor, "CREATE_INDUSTRY_PROJECT", "IndustryProject", saved.getId(), "Created industry project: " + saved.getTitle());
        return toProjectDto(saved);
    }

    @Transactional
    public IndustryProjectDto updateProject(Long id, IndustryProjectRequest req, String actor) {
        IndustryProject project = industryProjectRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IndustryProject", id));

        if (req.getCompanyId() != null) {
            project.setCompany(findCompany(req.getCompanyId()));
        }
        if (req.getTitle() != null) project.setTitle(req.getTitle());
        if (req.getProblemStatement() != null) project.setProblemStatement(req.getProblemStatement());
        if (req.getDepartment() != null) project.setDepartment(req.getDepartment());
        if (req.getMentor() != null) project.setMentor(req.getMentor());
        if (req.getTeamStudents() != null) project.setTeamStudents(req.getTeamStudents());
        if (req.getStatus() != null) project.setStatus(req.getStatus());
        if (req.getOutcome() != null) project.setOutcome(req.getOutcome());
        if (req.getAcademicYear() != null) project.setAcademicYear(req.getAcademicYear());
        if (req.getDriveFileId() != null) project.setDriveFileId(req.getDriveFileId());

        IndustryProject saved = industryProjectRepo.save(project);
        logActivity(actor, "UPDATE_INDUSTRY_PROJECT", "IndustryProject", saved.getId(), "Updated project: " + saved.getTitle() + " [" + saved.getStatus() + "]");
        return toProjectDto(saved);
    }

    // =========================================================================
    // 9. ADVISORY BOARD MEMBERS
    // =========================================================================

    @Transactional(readOnly = true)
    public List<AdvisoryMemberDto> getAdvisoryMembers(AdvisoryMember.MemberStatus status) {
        List<AdvisoryMember> members = advisoryMemberRepo.findAllByOrderByCreatedAtDesc();
        return members.stream()
                .filter(m -> status == null || m.getStatus() == status)
                .map(this::toAdvisoryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdvisoryMemberDto getAdvisoryMemberById(Long id) {
        return advisoryMemberRepo.findById(id)
                .map(this::toAdvisoryDto)
                .orElseThrow(() -> new ResourceNotFoundException("AdvisoryMember", id));
    }

    @Transactional
    public AdvisoryMemberDto createAdvisoryMember(AdvisoryMemberRequest req, String actor) {
        Company company = null;
        if (req.getCompanyId() != null) {
            company = findCompany(req.getCompanyId());
        }

        AdvisoryMember member = AdvisoryMember.builder()
                .name(req.getName())
                .designation(req.getDesignation())
                .company(company)
                .companyName(company != null ? company.getName() : req.getCompanyName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .domain(req.getDomain())
                .bio(req.getBio())
                .tenureStart(req.getTenureStart())
                .tenureEnd(req.getTenureEnd())
                .status(req.getStatus() != null ? req.getStatus() : AdvisoryMember.MemberStatus.ACTIVE)
                .build();

        AdvisoryMember saved = advisoryMemberRepo.save(member);
        logActivity(actor, "CREATE_ADVISORY_MEMBER", "AdvisoryMember", saved.getId(), "Inducted advisory board member: " + saved.getName());
        return toAdvisoryDto(saved);
    }

    @Transactional
    public AdvisoryMemberDto updateAdvisoryMember(Long id, AdvisoryMemberRequest req, String actor) {
        AdvisoryMember member = advisoryMemberRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdvisoryMember", id));

        if (req.getCompanyId() != null) {
            Company company = findCompany(req.getCompanyId());
            member.setCompany(company);
            member.setCompanyName(company.getName());
        } else if (req.getCompanyName() != null) {
            member.setCompanyName(req.getCompanyName());
        }

        if (req.getName() != null) member.setName(req.getName());
        if (req.getDesignation() != null) member.setDesignation(req.getDesignation());
        if (req.getEmail() != null) member.setEmail(req.getEmail());
        if (req.getPhone() != null) member.setPhone(req.getPhone());
        if (req.getDomain() != null) member.setDomain(req.getDomain());
        if (req.getBio() != null) member.setBio(req.getBio());
        if (req.getTenureStart() != null) member.setTenureStart(req.getTenureStart());
        if (req.getTenureEnd() != null) member.setTenureEnd(req.getTenureEnd());
        if (req.getStatus() != null) member.setStatus(req.getStatus());

        AdvisoryMember saved = advisoryMemberRepo.save(member);
        logActivity(actor, "UPDATE_ADVISORY_MEMBER", "AdvisoryMember", saved.getId(), "Updated advisory member: " + saved.getName());
        return toAdvisoryDto(saved);
    }

    // =========================================================================
    // 10. ALUMNI CORPORATE NETWORK
    // =========================================================================

    @Transactional(readOnly = true)
    public List<AlumniContactDto> getAlumniContacts(String department, String company, Boolean willingToMentor, Boolean willingToRefer) {
        List<AlumniContact> list = alumniContactRepo.findAllByOrderByCreatedAtDesc();
        return list.stream()
                .filter(a -> department == null || department.trim().isEmpty() || (a.getDepartment() != null && a.getDepartment().equalsIgnoreCase(department)))
                .filter(a -> company == null || company.trim().isEmpty() || (a.getCurrentCompany() != null && a.getCurrentCompany().toLowerCase().contains(company.toLowerCase())))
                .filter(a -> willingToMentor == null || Objects.equals(a.getWillingToMentor(), willingToMentor))
                .filter(a -> willingToRefer == null || Objects.equals(a.getWillingToRefer(), willingToRefer))
                .map(this::toAlumniDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AlumniContactDto getAlumniContactById(Long id) {
        return alumniContactRepo.findById(id)
                .map(this::toAlumniDto)
                .orElseThrow(() -> new ResourceNotFoundException("AlumniContact", id));
    }

    @Transactional
    public AlumniContactDto createAlumniContact(AlumniContactRequest req, String actor) {
        AlumniContact contact = AlumniContact.builder()
                .name(req.getName())
                .batch(req.getBatch())
                .degree(req.getDegree())
                .department(req.getDepartment())
                .currentCompany(req.getCurrentCompany())
                .currentRole(req.getCurrentRole())
                .email(req.getEmail())
                .phone(req.getPhone())
                .linkedinUrl(req.getLinkedinUrl())
                .willingToMentor(req.getWillingToMentor() != null ? req.getWillingToMentor() : true)
                .willingToRefer(req.getWillingToRefer() != null ? req.getWillingToRefer() : true)
                .build();

        AlumniContact saved = alumniContactRepo.save(contact);
        logActivity(actor, "CREATE_ALUMNI_CONTACT", "AlumniContact", saved.getId(), "Added corporate alumni contact: " + saved.getName() + " (" + saved.getCurrentCompany() + ")");
        return toAlumniDto(saved);
    }

    @Transactional
    public AlumniContactDto updateAlumniContact(Long id, AlumniContactRequest req, String actor) {
        AlumniContact contact = alumniContactRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AlumniContact", id));

        if (req.getName() != null) contact.setName(req.getName());
        if (req.getBatch() != null) contact.setBatch(req.getBatch());
        if (req.getDegree() != null) contact.setDegree(req.getDegree());
        if (req.getDepartment() != null) contact.setDepartment(req.getDepartment());
        if (req.getCurrentCompany() != null) contact.setCurrentCompany(req.getCurrentCompany());
        if (req.getCurrentRole() != null) contact.setCurrentRole(req.getCurrentRole());
        if (req.getEmail() != null) contact.setEmail(req.getEmail());
        if (req.getPhone() != null) contact.setPhone(req.getPhone());
        if (req.getLinkedinUrl() != null) contact.setLinkedinUrl(req.getLinkedinUrl());
        if (req.getWillingToMentor() != null) contact.setWillingToMentor(req.getWillingToMentor());
        if (req.getWillingToRefer() != null) contact.setWillingToRefer(req.getWillingToRefer());

        AlumniContact saved = alumniContactRepo.save(contact);
        logActivity(actor, "UPDATE_ALUMNI_CONTACT", "AlumniContact", saved.getId(), "Updated corporate alumni contact: " + saved.getName());
        return toAlumniDto(saved);
    }

    // =========================================================================
    // 11. REPORTS & ENGAGEMENT ANALYTICS
    // =========================================================================

    @Transactional(readOnly = true)
    public IndustryEngagementSummaryDto getEngagementSummary() {
        List<IndustryMou> allMous = mouRepo.findAll();
        List<Internship> allInternships = internshipRepo.findAll();
        List<IndustryVisit> allVisits = visitRepo.findAll();
        List<GuestLecture> allLectures = guestLectureRepo.findAll();
        List<IndustryProject> allProjects = industryProjectRepo.findAll();

        Set<String> depts = new LinkedHashSet<>(List.of("CSE", "ECE", "ME", "CE", "EEE", "AIDS"));
        allVisits.forEach(v -> { if (v.getDepartment() != null && !v.getDepartment().isBlank()) depts.add(v.getDepartment().trim().toUpperCase()); });
        allProjects.forEach(p -> { if (p.getDepartment() != null && !p.getDepartment().isBlank()) depts.add(p.getDepartment().trim().toUpperCase()); });
        extractDepts(allMous.stream().map(IndustryMou::getDepartments).toList(), depts);
        extractDepts(allInternships.stream().map(Internship::getDepartments).toList(), depts);
        extractDepts(allLectures.stream().map(GuestLecture::getDepartments).toList(), depts);

        List<DepartmentEngagementDto> deptEngagementList = new ArrayList<>();
        for (String dept : depts) {
            long mouCount = allMous.stream().filter(m -> matchesDept(m.getDepartments(), dept)).count();
            long internshipCount = allInternships.stream().filter(i -> matchesDept(i.getDepartments(), dept)).count();
            long visitCount = allVisits.stream().filter(v -> matchesSingleDept(v.getDepartment(), dept)).count();
            long lectureCount = allLectures.stream().filter(l -> matchesDept(l.getDepartments(), dept)).count();
            long projectCount = allProjects.stream().filter(p -> matchesSingleDept(p.getDepartment(), dept)).count();
            long total = mouCount + internshipCount + visitCount + lectureCount + projectCount;

            deptEngagementList.add(DepartmentEngagementDto.builder()
                    .department(dept)
                    .mous(mouCount)
                    .internships(internshipCount)
                    .visits(visitCount)
                    .lectures(lectureCount)
                    .projects(projectCount)
                    .totalEngagement(total)
                    .build());
        }

        DepartmentEngagementDto totals = DepartmentEngagementDto.builder()
                .department("TOTAL")
                .mous(allMous.size())
                .internships(allInternships.size())
                .visits(allVisits.size())
                .lectures(allLectures.size())
                .projects(allProjects.size())
                .totalEngagement((long) allMous.size() + allInternships.size() + allVisits.size() + allLectures.size() + allProjects.size())
                .build();

        return IndustryEngagementSummaryDto.builder()
                .departments(deptEngagementList)
                .totals(totals)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new BadRequestException("Export type parameter is required (mou, internship, visit)");
        }
        String cleanType = type.trim().toLowerCase();
        StringBuilder sb = new StringBuilder();

        switch (cleanType) {
            case "mou", "mous" -> {
                List<IndustryMou> mous = mouRepo.findAll();
                sb.append("ID,Company,Title,SignedDate,ValidFrom,ValidTo,Status,Departments,Scope\n");
                for (IndustryMou m : mous) {
                    sb.append(escapeCsv(m.getId())).append(",")
                      .append(escapeCsv(m.getCompany() != null ? m.getCompany().getName() : "")).append(",")
                      .append(escapeCsv(m.getTitle())).append(",")
                      .append(escapeCsv(m.getSignedDate())).append(",")
                      .append(escapeCsv(m.getValidFrom())).append(",")
                      .append(escapeCsv(m.getValidTo())).append(",")
                      .append(escapeCsv(m.getStatus())).append(",")
                      .append(escapeCsv(m.getDepartments())).append(",")
                      .append(escapeCsv(m.getScope())).append("\n");
                }
            }
            case "internship", "internships" -> {
                List<Internship> list = internshipRepo.findAll();
                sb.append("ID,Company,Title,Duration,Stipend,Mode,Seats,Deadline,Status,Departments\n");
                for (Internship i : list) {
                    sb.append(escapeCsv(i.getId())).append(",")
                      .append(escapeCsv(i.getCompany() != null ? i.getCompany().getName() : "")).append(",")
                      .append(escapeCsv(i.getTitle())).append(",")
                      .append(escapeCsv(i.getDuration())).append(",")
                      .append(escapeCsv(i.getStipend())).append(",")
                      .append(escapeCsv(i.getMode())).append(",")
                      .append(escapeCsv(i.getSeats())).append(",")
                      .append(escapeCsv(i.getDeadline())).append(",")
                      .append(escapeCsv(i.getStatus())).append(",")
                      .append(escapeCsv(i.getDepartments())).append("\n");
                }
            }
            case "visit", "visits" -> {
                List<IndustryVisit> list = visitRepo.findAll();
                sb.append("ID,Company,Department,Proposer,VisitDate,StudentCount,Stage,Status,Location\n");
                for (IndustryVisit v : list) {
                    sb.append(escapeCsv(v.getId())).append(",")
                      .append(escapeCsv(v.getCompany() != null ? v.getCompany().getName() : "")).append(",")
                      .append(escapeCsv(v.getDepartment())).append(",")
                      .append(escapeCsv(v.getProposedBy() != null ? v.getProposedBy().getName() : "")).append(",")
                      .append(escapeCsv(v.getDate())).append(",")
                      .append(escapeCsv(v.getStudentCount())).append(",")
                      .append(escapeCsv(v.getStage())).append(",")
                      .append(escapeCsv(v.getStatus())).append(",")
                      .append(escapeCsv(v.getLocation())).append("\n");
                }
            }
            default -> throw new BadRequestException("Unsupported export type: '" + type + "'. Supported types: mou, internship, visit");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportPdf(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new BadRequestException("Export type parameter is required (mou, internship, visit)");
        }
        String cleanType = type.trim().toLowerCase();
        switch (cleanType) {
            case "mou", "mous" -> {
                List<IndustryMou> mous = mouRepo.findAll();
                return pdfService.generateIndustryReportPdf("mou", mous);
            }
            case "internship", "internships" -> {
                List<Internship> list = internshipRepo.findAll();
                return pdfService.generateIndustryReportPdf("internship", list);
            }
            case "visit", "visits" -> {
                List<IndustryVisit> list = visitRepo.findAll();
                return pdfService.generateIndustryReportPdf("visit", list);
            }
            default -> throw new BadRequestException("Unsupported export type: '" + type + "'. Supported types: mou, internship, visit");
        }
    }

    // =========================================================================
    // 12. ACTIVITY LOGGING & HELPERS
    // =========================================================================

    public void logActivity(String actor, String action, String entityType, Long entityId, String details) {
        try {
            IndustryActivityLog logEntry = IndustryActivityLog.builder()
                    .actor(actor != null && !actor.trim().isEmpty() ? actor : "ICH_USER")
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .timestamp(LocalDateTime.now())
                    .build();
            activityLogRepo.save(logEntry);
        } catch (Exception e) {
            log.warn("Could not save industry activity log: {}", e.getMessage());
        }
    }

    private Company findCompany(Long id) {
        return companyRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));
    }

    private IndustryMou findMou(Long id) {
        return mouRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IndustryMou", id));
    }

    private Internship findInternship(Long id) {
        return internshipRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Internship", id));
    }

    private InternshipApplication findApplication(Long id) {
        return applicationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("InternshipApplication", id));
    }

    public IndustryVisit findVisit(Long id) {
        return visitRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IndustryVisit", id));
    }

    private boolean isDepartmentEligible(String configuredDepts, String studentDept) {
        if (configuredDepts == null || configuredDepts.trim().isEmpty()) {
            return true; // Open to all branches
        }
        if (studentDept == null || studentDept.trim().isEmpty()) {
            return true;
        }
        return Arrays.stream(configuredDepts.split(","))
                .map(String::trim)
                .anyMatch(d -> d.equalsIgnoreCase(studentDept.trim()));
    }

    private CompanyDto toCompanyDto(Company c) {
        if (c == null) return null;
        return CompanyDto.builder()
                .id(c.getId())
                .name(c.getName())
                .logo(c.getLogo())
                .location(c.getLocation())
                .description(c.getDescription())
                .sector(c.getSector())
                .website(c.getWebsite())
                .hrName(c.getHrName())
                .hrEmail(c.getHrEmail())
                .hrPhone(c.getHrPhone())
                .partnershipStage(c.getPartnershipStage())
                .tier(c.getTier())
                .placementReady(c.getPlacementReady())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private IndustryMouDto toMouDto(IndustryMou m) {
        if (m == null) return null;
        return IndustryMouDto.builder()
                .id(m.getId())
                .companyId(m.getCompany() != null ? m.getCompany().getId() : null)
                .companyName(m.getCompany() != null ? m.getCompany().getName() : null)
                .companyLogo(m.getCompany() != null ? m.getCompany().getLogo() : null)
                .title(m.getTitle())
                .signedDate(m.getSignedDate())
                .validFrom(m.getValidFrom())
                .validTo(m.getValidTo())
                .scope(m.getScope())
                .status(m.getStatus())
                .driveFileId(m.getDriveFileId())
                .departments(m.getDepartments())
                .documentUrl(m.getDocumentUrl())
                .remarks(m.getRemarks())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    private InternshipDto toInternshipDto(Internship i) {
        if (i == null) return null;
        long totalApplicants = applicationRepo.countByInternshipId(i.getId());
        return InternshipDto.builder()
                .id(i.getId())
                .companyId(i.getCompany() != null ? i.getCompany().getId() : null)
                .companyName(i.getCompany() != null ? i.getCompany().getName() : null)
                .companyLogo(i.getCompany() != null ? i.getCompany().getLogo() : null)
                .title(i.getTitle())
                .description(i.getDescription())
                .requiredSkills(i.getRequiredSkills())
                .duration(i.getDuration())
                .stipend(i.getStipend())
                .mode(i.getMode())
                .departments(i.getDepartments())
                .seats(i.getSeats())
                .deadline(i.getDeadline())
                .status(i.getStatus())
                .location(i.getLocation())
                .totalApplicants(totalApplicants)
                .createdAt(i.getCreatedAt())
                .build();
    }

    private InternshipApplicationDto toApplicationDto(InternshipApplication a) {
        if (a == null) return null;
        return InternshipApplicationDto.builder()
                .id(a.getId())
                .internshipId(a.getInternship() != null ? a.getInternship().getId() : null)
                .internshipTitle(a.getInternship() != null ? a.getInternship().getTitle() : null)
                .companyName(a.getInternship() != null && a.getInternship().getCompany() != null ? a.getInternship().getCompany().getName() : null)
                .studentId(a.getStudent() != null ? a.getStudent().getId() : null)
                .studentName(a.getStudent() != null ? a.getStudent().getName() : null)
                .studentEmail(a.getStudent() != null ? a.getStudent().getEmail() : null)
                .studentRoll(a.getStudent() != null ? a.getStudent().getRollNumber() : null)
                .studentDepartment(a.getStudent() != null ? a.getStudent().getDepartment() : null)
                .studentSemester(a.getStudent() != null ? a.getStudent().getSemester() : null)
                .resumeUrl(a.getResumeUrl())
                .status(a.getStatus())
                .certificateUrl(a.getCertificateUrl())
                .feedback(a.getFeedback())
                .appliedAt(a.getAppliedAt())
                .build();
    }

    private IndustryActivityLogDto toActivityLogDto(IndustryActivityLog log) {
        if (log == null) return null;
        return IndustryActivityLogDto.builder()
                .id(log.getId())
                .actor(log.getActor())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .details(log.getDetails())
                .timestamp(log.getTimestamp())
                .build();
    }

    private IndustryVisitDto toVisitDto(IndustryVisit v) {
        if (v == null) return null;
        return IndustryVisitDto.builder()
                .id(v.getId())
                .companyId(v.getCompany() != null ? v.getCompany().getId() : null)
                .companyName(v.getCompany() != null ? v.getCompany().getName() : null)
                .companyLogo(v.getCompany() != null ? v.getCompany().getLogo() : null)
                .proposedById(v.getProposedBy() != null ? v.getProposedBy().getId() : null)
                .proposedByName(v.getProposedBy() != null ? v.getProposedBy().getName() : null)
                .department(v.getDepartment())
                .date(v.getDate())
                .studentCount(v.getStudentCount())
                .purpose(v.getPurpose())
                .location(v.getLocation())
                .overnight(v.getOvernight())
                .outsideKerala(v.getOutsideKerala())
                .stage(v.getStage())
                .status(v.getStatus())
                .ichStatus(v.getIchStatus())
                .ichRemark(v.getIchRemark())
                .ichApprovedBy(v.getIchApprovedBy())
                .ichApprovedAt(v.getIchApprovedAt())
                .hodStatus(v.getHodStatus())
                .hodRemark(v.getHodRemark())
                .hodApprovedBy(v.getHodApprovedBy())
                .hodApprovedAt(v.getHodApprovedAt())
                .principalStatus(v.getPrincipalStatus())
                .principalRemark(v.getPrincipalRemark())
                .principalApprovedAt(v.getPrincipalApprovedAt())
                .pdfPath(v.getPdfPath())
                .reportDriveFileId(v.getReportDriveFileId())
                .createdAt(v.getCreatedAt())
                .build();
    }

    private GuestLectureDto toLectureDto(GuestLecture l) {
        if (l == null) return null;
        return GuestLectureDto.builder()
                .id(l.getId())
                .speaker(l.getSpeaker())
                .designation(l.getDesignation())
                .companyId(l.getCompany() != null ? l.getCompany().getId() : null)
                .companyName(l.getCompany() != null ? l.getCompany().getName() : l.getCompanyName())
                .topic(l.getTopic())
                .date(l.getDate())
                .time(l.getTime())
                .venue(l.getVenue())
                .departments(l.getDepartments())
                .attendance(l.getAttendance())
                .feedbackAvg(l.getFeedbackAvg())
                .coordinatorId(l.getCoordinator() != null ? l.getCoordinator().getId() : null)
                .coordinatorName(l.getCoordinator() != null ? l.getCoordinator().getName() : null)
                .status(l.getStatus())
                .driveFileId(l.getDriveFileId())
                .createdAt(l.getCreatedAt())
                .build();
    }

    private IndustryProjectDto toProjectDto(IndustryProject p) {
        if (p == null) return null;
        return IndustryProjectDto.builder()
                .id(p.getId())
                .companyId(p.getCompany() != null ? p.getCompany().getId() : null)
                .companyName(p.getCompany() != null ? p.getCompany().getName() : null)
                .companyLogo(p.getCompany() != null ? p.getCompany().getLogo() : null)
                .title(p.getTitle())
                .problemStatement(p.getProblemStatement())
                .department(p.getDepartment())
                .mentor(p.getMentor())
                .teamStudents(p.getTeamStudents())
                .status(p.getStatus())
                .outcome(p.getOutcome())
                .academicYear(p.getAcademicYear())
                .driveFileId(p.getDriveFileId())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private AdvisoryMemberDto toAdvisoryDto(AdvisoryMember a) {
        if (a == null) return null;
        return AdvisoryMemberDto.builder()
                .id(a.getId())
                .name(a.getName())
                .designation(a.getDesignation())
                .companyId(a.getCompany() != null ? a.getCompany().getId() : null)
                .companyName(a.getCompany() != null ? a.getCompany().getName() : a.getCompanyName())
                .email(a.getEmail())
                .phone(a.getPhone())
                .domain(a.getDomain())
                .bio(a.getBio())
                .tenureStart(a.getTenureStart())
                .tenureEnd(a.getTenureEnd())
                .status(a.getStatus())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private AlumniContactDto toAlumniDto(AlumniContact a) {
        if (a == null) return null;
        return AlumniContactDto.builder()
                .id(a.getId())
                .name(a.getName())
                .batch(a.getBatch())
                .degree(a.getDegree())
                .department(a.getDepartment())
                .currentCompany(a.getCurrentCompany())
                .currentRole(a.getCurrentRole())
                .email(a.getEmail())
                .phone(a.getPhone())
                .linkedinUrl(a.getLinkedinUrl())
                .willingToMentor(a.getWillingToMentor())
                .willingToRefer(a.getWillingToRefer())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private boolean matchesDept(String deptsCsv, String targetDept) {
        if (deptsCsv == null || deptsCsv.trim().isEmpty()) return false;
        for (String part : deptsCsv.split(",")) {
            String p = part.trim();
            if (p.equalsIgnoreCase("ALL") || p.equalsIgnoreCase(targetDept)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesSingleDept(String dept, String targetDept) {
        if (dept == null) return false;
        return dept.trim().equalsIgnoreCase(targetDept) || dept.trim().equalsIgnoreCase("ALL");
    }

    private void extractDepts(List<String> rawList, Set<String> targetSet) {
        for (String raw : rawList) {
            if (raw == null || raw.isBlank()) continue;
            for (String p : raw.split(",")) {
                String clean = p.trim().toUpperCase();
                if (!clean.isEmpty() && !clean.equals("ALL") && !clean.equals("ALL DEPARTMENTS")) {
                    targetSet.add(clean);
                }
            }
        }
    }

    private String escapeCsv(Object val) {
        if (val == null) return "";
        String str = String.valueOf(val);
        if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }
}
