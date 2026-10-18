package com.edusync.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Industrial Visit proposal entity.
 * Multi-stage approval flow: Proposed by Faculty/HOD → ICH Review → HOD Review → Principal Sanction.
 */
@Entity
@Table(name = "industry_visits")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndustryVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposed_by_id", nullable = false)
    private User proposedBy;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "student_count")
    private Integer studentCount;

    @Column(columnDefinition = "TEXT")
    private String purpose;

    @Column
    private String location;

    @Column
    @Builder.Default
    private Boolean overnight = false;

    @Column(name = "outside_kerala")
    @Builder.Default
    private Boolean outsideKerala = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VisitStage stage = VisitStage.ICH_REVIEW;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VisitStatus status = VisitStatus.PENDING;

    // --- ICH Stage ---
    @Enumerated(EnumType.STRING)
    @Column(name = "ich_status")
    @Builder.Default
    private ApprovalStatus ichStatus = ApprovalStatus.PENDING;

    @Column(name = "ich_remark")
    private String ichRemark;

    @Column(name = "ich_approved_by")
    private String ichApprovedBy;

    @Column(name = "ich_approved_at")
    private LocalDateTime ichApprovedAt;

    // --- HOD Stage ---
    @Enumerated(EnumType.STRING)
    @Column(name = "hod_status")
    @Builder.Default
    private ApprovalStatus hodStatus = ApprovalStatus.PENDING;

    @Column(name = "hod_remark")
    private String hodRemark;

    @Column(name = "hod_approved_by")
    private String hodApprovedBy;

    @Column(name = "hod_approved_at")
    private LocalDateTime hodApprovedAt;

    // --- Principal Stage ---
    @Enumerated(EnumType.STRING)
    @Column(name = "principal_status")
    @Builder.Default
    private ApprovalStatus principalStatus = ApprovalStatus.PENDING;

    @Column(name = "principal_remark")
    private String principalRemark;

    @Column(name = "principal_approved_at")
    private LocalDateTime principalApprovedAt;

    // --- Sanction Letter / Post-visit Report ---
    @Column(name = "pdf_path")
    private String pdfPath;

    @Column(name = "report_drive_file_id")
    private String reportDriveFileId;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum VisitStage {
        ICH_REVIEW,
        HOD_REVIEW,
        PRINCIPAL_REVIEW,
        COMPLETED
    }

    public enum VisitStatus {
        PENDING,
        APPROVED,
        REJECTED,
        CANCELLED
    }

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED,
        NOT_REQUIRED
    }
}
