package com.edusync.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Shared Partner Company entity across Placement Officer and Industrial Connect Head.
 */
@Entity
@Table(name = "companies")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String logo;

    @Column
    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private String sector;

    @Column
    private String website;

    @Column(name = "hr_name")
    private String hrName;

    @Column(name = "hr_email")
    private String hrEmail;

    @Column(name = "hr_phone")
    private String hrPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "partnership_stage")
    @Builder.Default
    private PartnershipStage partnershipStage = PartnershipStage.LEAD;

    @Column
    private String tier;

    @Column(name = "placement_ready")
    @Builder.Default
    private Boolean placementReady = false;

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

    public enum PartnershipStage {
        LEAD,
        CONTACTED,
        NEGOTIATING,
        MOU_SIGNED,
        ACTIVE
    }
}
