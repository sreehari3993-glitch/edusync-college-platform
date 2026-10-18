package com.edusync.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Internship opportunity entity sourced from industry partners.
 */
@Entity
@Table(name = "internships")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Internship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "required_skills", columnDefinition = "TEXT")
    private String requiredSkills;

    @Column
    private String duration;          // e.g. "3 Months", "6 Months"

    @Column
    private String stipend;           // e.g. "15000/month" or "Unpaid"

    @Column
    private String mode;              // "REMOTE", "HYBRID", "ON_SITE"

    @Column
    private String departments;       // comma-separated: "CSE,ECE"

    @Column
    private Integer seats;

    @Column
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InternshipStatus status = InternshipStatus.OPEN;

    @Column
    private String location;

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

    public enum InternshipStatus {
        OPEN,
        CLOSED,
        ARCHIVED
    }
}
