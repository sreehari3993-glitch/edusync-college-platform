package com.edusync.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Corporate Alumni Network Contact entity.
 * Connects alumni working in top companies for internships, mentorship, and guest lectures.
 */
@Entity
@Table(name = "alumni_contacts")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlumniContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String batch;             // e.g. "2020", "2022"

    @Column
    private String degree;            // e.g. "B.Tech CSE"

    @Column
    private String department;

    @Column(name = "current_company")
    private String currentCompany;

    @Column(name = "current_role")
    private String currentRole;

    @Column
    private String email;

    @Column
    private String phone;

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @Column(name = "willing_to_mentor")
    @Builder.Default
    private Boolean willingToMentor = true;

    @Column(name = "willing_to_refer")
    @Builder.Default
    private Boolean willingToRefer = true;

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
}
