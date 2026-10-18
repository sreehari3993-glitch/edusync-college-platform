package com.edusync.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Industry Memorandum of Understanding (MoU) entity.
 * Managed by Industrial Connect Head; signed and approved by Principal.
 */
@Entity
@Table(name = "industry_mous")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndustryMou {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String title;

    @Column(name = "signed_date")
    private LocalDate signedDate;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(columnDefinition = "TEXT")
    private String scope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MouStatus status = MouStatus.DRAFT;

    @Column(name = "drive_file_id")
    private String driveFileId;

    @Column
    private String departments;    // e.g. "CSE,ECE,ME"

    @Column(name = "document_url")
    private String documentUrl;

    @Column(columnDefinition = "TEXT")
    private String remarks;

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

    public enum MouStatus {
        DRAFT,
        PRINCIPAL_APPROVAL,
        ACTIVE,
        EXPIRING,
        EXPIRED,
        TERMINATED
    }
}
