package com.edusync.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Industry Advisory Board Member entity.
 */
@Entity
@Table(name = "advisory_members")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdvisoryMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String designation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "company_name")
    private String companyName;

    @Column
    private String email;

    @Column
    private String phone;

    @Column
    private String domain;            // e.g. "Cloud Computing & DevOps", "Automotive Embedded"

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "tenure_start")
    private LocalDate tenureStart;

    @Column(name = "tenure_end")
    private LocalDate tenureEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MemberStatus status = MemberStatus.ACTIVE;

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

    public enum MemberStatus {
        ACTIVE,
        INACTIVE
    }
}
