package com.edusync.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Industry-Sponsored Student Projects entity (capstone / consultancy).
 */
@Entity
@Table(name = "industry_projects")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndustryProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false)
    private String title;

    @Column(name = "problem_statement", columnDefinition = "TEXT", nullable = false)
    private String problemStatement;

    @Column
    private String department;

    @Column
    private String mentor;            // Industry / Faculty Mentor

    @Column(name = "team_students", columnDefinition = "TEXT")
    private String teamStudents;      // Names or roll numbers of students

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.PROPOSED;

    @Column(columnDefinition = "TEXT")
    private String outcome;

    @Column(name = "academic_year")
    private String academicYear;      // e.g. "2024-2025"

    @Column(name = "drive_file_id")
    private String driveFileId;

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

    public enum ProjectStatus {
        PROPOSED,
        ONGOING,
        COMPLETED,
        TERMINATED
    }
}
