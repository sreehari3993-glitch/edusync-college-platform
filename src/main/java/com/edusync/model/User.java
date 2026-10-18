package com.edusync.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

/**
 * User entity — base table for all roles.
 * Roles: STUDENT, FACULTY, HOD, PRINCIPAL, PLACEMENT_OFFICER, ADMIN
 */
@Entity
@Table(name = "users")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @NotBlank
    @Column(nullable = false)
    private String password;   // bcrypt hashed

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "roll_number")
    private String rollNumber;       // for students

    @Column(name = "employee_id")
    private String employeeId;       // for faculty/staff

    @Column
    private String department;       // CSE, ECE, MECH, FT, etc.

    @Column
    private String semester;         // e.g. "S4" — students only

    @Column
    private String section;          // e.g. "A", "B"

    @Column
    private String phone;

    @Column(name = "parent_email")
    private String parentEmail;

    @Column(name = "parent_phone")
    private String parentPhone;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "advisor_id")
    @JsonIgnoreProperties({"advisor", "password", "hibernateLazyInitializer", "handler"})
    private User advisor;

    @Column(name = "profile_pic")
    private String profilePic;       // file path

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

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

    public enum Role {
        STUDENT,
        FACULTY,
        HOD,
        PRINCIPAL,
        PLACEMENT_OFFICER,
        ADMIN,
        INDUSTRIAL_CONNECT_HEAD
    }
}
