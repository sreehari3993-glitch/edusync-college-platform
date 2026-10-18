package com.edusync.dto;

import com.edusync.model.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class IndustryDto {

    // =========================================================================
    // 1. COMPANY DTOs
    // =========================================================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompanyRequest {
        @NotBlank(message = "Company name is required")
        private String name;
        private String logo;
        private String location;
        private String description;
        private String sector;
        private String website;
        private String hrName;
        private String hrEmail;
        private String hrPhone;
        private Company.PartnershipStage partnershipStage;
        private String tier;
        private Boolean placementReady;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompanyDto {
        private Long id;
        private String name;
        private String logo;
        private String location;
        private String description;
        private String sector;
        private String website;
        private String hrName;
        private String hrEmail;
        private String hrPhone;
        private Company.PartnershipStage partnershipStage;
        private String tier;
        private Boolean placementReady;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    // =========================================================================
    // 2. MOU DTOs
    // =========================================================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IndustryMouRequest {
        @NotNull(message = "Company ID is required")
        private Long companyId;
        @NotBlank(message = "Title is required")
        private String title;
        private LocalDate signedDate;
        private LocalDate validFrom;
        private LocalDate validTo;
        private String scope;
        private IndustryMou.MouStatus status;
        private String driveFileId;
        private String departments;
        private String documentUrl;
        private String remarks;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IndustryMouDto {
        private Long id;
        private Long companyId;
        private String companyName;
        private String companyLogo;
        private String title;
        private LocalDate signedDate;
        private LocalDate validFrom;
        private LocalDate validTo;
        private String scope;
        private IndustryMou.MouStatus status;
        private String driveFileId;
        private String departments;
        private String documentUrl;
        private String remarks;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    // =========================================================================
    // 3. INTERNSHIP DTOs
    // =========================================================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InternshipRequest {
        @NotNull(message = "Company ID is required")
        private Long companyId;
        @NotBlank(message = "Title is required")
        private String title;
        private String description;
        private String requiredSkills;
        private String duration;
        private String stipend;
        private String mode;
        private String departments;
        private Integer seats;
        private LocalDate deadline;
        private Internship.InternshipStatus status;
        private String location;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InternshipDto {
        private Long id;
        private Long companyId;
        private String companyName;
        private String companyLogo;
        private String title;
        private String description;
        private String requiredSkills;
        private String duration;
        private String stipend;
        private String mode;
        private String departments;
        private Integer seats;
        private LocalDate deadline;
        private Internship.InternshipStatus status;
        private String location;
        private Long totalApplicants;
        private LocalDateTime createdAt;
    }

    // =========================================================================
    // 4. INTERNSHIP APPLICATION DTOs
    // =========================================================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InternshipApplicationRequest {
        private String resumeUrl;
        private String feedback;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InternshipApplicationStatusRequest {
        @NotNull
        private InternshipApplication.ApplicationStatus status;
        private String feedback;
        private String certificateUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InternshipApplicationDto {
        private Long id;
        private Long internshipId;
        private String internshipTitle;
        private String companyName;
        private Long studentId;
        private String studentName;
        private String studentEmail;
        private String studentRoll;
        private String studentDepartment;
        private String studentSemester;
        private String resumeUrl;
        private InternshipApplication.ApplicationStatus status;
        private String certificateUrl;
        private String feedback;
        private LocalDateTime appliedAt;
    }

    // =========================================================================
    // 5. INDUSTRY VISIT DTOs
    // =========================================================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IndustryVisitRequest {
        @NotNull(message = "Company ID is required")
        private Long companyId;
        @NotBlank(message = "Department is required")
        private String department;
        @NotNull(message = "Visit date is required")
        private LocalDate date;
        private Integer studentCount;
        private String purpose;
        private String location;
        private Boolean overnight;
        private Boolean outsideKerala;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VisitActionRequest {
        @NotNull
        private IndustryVisit.ApprovalStatus status; // APPROVED, REJECTED
        private String remark;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IndustryVisitDto {
        private Long id;
        private Long companyId;
        private String companyName;
        private String companyLogo;
        private Long proposedById;
        private String proposedByName;
        private String department;
        private LocalDate date;
        private Integer studentCount;
        private String purpose;
        private String location;
        private Boolean overnight;
        private Boolean outsideKerala;
        private IndustryVisit.VisitStage stage;
        private IndustryVisit.VisitStatus status;

        // Stage reviews
        private IndustryVisit.ApprovalStatus ichStatus;
        private String ichRemark;
        private String ichApprovedBy;
        private LocalDateTime ichApprovedAt;

        private IndustryVisit.ApprovalStatus hodStatus;
        private String hodRemark;
        private String hodApprovedBy;
        private LocalDateTime hodApprovedAt;

        private IndustryVisit.ApprovalStatus principalStatus;
        private String principalRemark;
        private LocalDateTime principalApprovedAt;

        private String pdfPath;
        private String reportDriveFileId;
        private LocalDateTime createdAt;
    }

    // =========================================================================
    // 6. GUEST LECTURE DTOs
    // =========================================================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GuestLectureRequest {
        @NotBlank(message = "Speaker is required")
        private String speaker;
        private String designation;
        private Long companyId;
        private String companyName;
        @NotBlank(message = "Topic is required")
        private String topic;
        @NotNull(message = "Date is required")
        private LocalDate date;
        private String time;
        private String venue;
        private String departments;
        private Integer attendance;
        private Double feedbackAvg;
        private Long coordinatorId;
        private GuestLecture.LectureStatus status;
        private String driveFileId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GuestLectureDto {
        private Long id;
        private String speaker;
        private String designation;
        private Long companyId;
        private String companyName;
        private String topic;
        private LocalDate date;
        private String time;
        private String venue;
        private String departments;
        private Integer attendance;
        private Double feedbackAvg;
        private Long coordinatorId;
        private String coordinatorName;
        private GuestLecture.LectureStatus status;
        private String driveFileId;
        private LocalDateTime createdAt;
    }

    // =========================================================================
    // 7. INDUSTRY PROJECT DTOs
    // =========================================================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IndustryProjectRequest {
        @NotNull(message = "Company ID is required")
        private Long companyId;
        @NotBlank(message = "Title is required")
        private String title;
        @NotBlank(message = "Problem statement is required")
        private String problemStatement;
        private String department;
        private String mentor;
        private String teamStudents;
        private IndustryProject.ProjectStatus status;
        private String outcome;
        private String academicYear;
        private String driveFileId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IndustryProjectDto {
        private Long id;
        private Long companyId;
        private String companyName;
        private String companyLogo;
        private String title;
        private String problemStatement;
        private String department;
        private String mentor;
        private String teamStudents;
        private IndustryProject.ProjectStatus status;
        private String outcome;
        private String academicYear;
        private String driveFileId;
        private LocalDateTime createdAt;
    }

    // =========================================================================
    // 8. ADVISORY MEMBER DTOs
    // =========================================================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdvisoryMemberRequest {
        @NotBlank(message = "Name is required")
        private String name;
        private String designation;
        private Long companyId;
        private String companyName;
        private String email;
        private String phone;
        private String domain;
        private String bio;
        private LocalDate tenureStart;
        private LocalDate tenureEnd;
        private AdvisoryMember.MemberStatus status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdvisoryMemberDto {
        private Long id;
        private String name;
        private String designation;
        private Long companyId;
        private String companyName;
        private String email;
        private String phone;
        private String domain;
        private String bio;
        private LocalDate tenureStart;
        private LocalDate tenureEnd;
        private AdvisoryMember.MemberStatus status;
        private LocalDateTime createdAt;
    }

    // =========================================================================
    // 9. ALUMNI CONTACT DTOs
    // =========================================================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AlumniContactRequest {
        @NotBlank(message = "Name is required")
        private String name;
        private String batch;
        private String degree;
        private String department;
        private String currentCompany;
        private String currentRole;
        private String email;
        private String phone;
        private String linkedinUrl;
        private Boolean willingToMentor;
        private Boolean willingToRefer;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AlumniContactDto {
        private Long id;
        private String name;
        private String batch;
        private String degree;
        private String department;
        private String currentCompany;
        private String currentRole;
        private String email;
        private String phone;
        private String linkedinUrl;
        private Boolean willingToMentor;
        private Boolean willingToRefer;
        private LocalDateTime createdAt;
    }

    // =========================================================================
    // 10. ACTIVITY LOG & DASHBOARD DTOs
    // =========================================================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IndustryActivityLogDto {
        private Long id;
        private String actor;
        private String action;
        private String entityType;
        private Long entityId;
        private String details;
        private LocalDateTime timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompanyStageUpdateRequest {
        @NotNull(message = "Partnership stage is required")
        private Company.PartnershipStage partnershipStage;
        private Boolean placementReady;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MouRenewalRequest {
        @NotNull(message = "New validFrom date is required")
        private LocalDate validFrom;
        @NotNull(message = "New validTo date is required")
        private LocalDate validTo;
        private String scope;
        private String remarks;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IndustryOverviewDto {
        private long activeMoUs;
        private long mousExpiringWithin60Days;
        private java.util.Map<String, Long> companiesByPartnershipStage;
        private long openInternships;
        private long applications;
        private long studentsCurrentlyInterning;
        private long totalCompanies;
        private List<IndustryMouDto> expiringMous;
        private List<IndustryVisitDto> pendingVisits;
        private List<IndustryActivityLogDto> recentActivities;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IndustryDashboardStatsDto {
        private long totalCompanies;
        private long activeMous;
        private long openInternships;
        private long pendingVisits;
        private long upcomingLectures;
        private long activeProjects;
        private long advisoryMembersCount;
        private long corporateAlumniCount;
        private List<IndustryActivityLogDto> recentActivities;
    }

    // =========================================================================
    // 11. REPORTS DTOs
    // =========================================================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DepartmentEngagementDto {
        private String department;
        private long mous;
        private long internships;
        private long visits;
        private long lectures;
        private long projects;
        private long totalEngagement;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IndustryEngagementSummaryDto {
        private List<DepartmentEngagementDto> departments;
        private DepartmentEngagementDto totals;
        private LocalDateTime generatedAt;
    }
}
