package com.edusync.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing an Advisor Request submitted by a Student to a Faculty member.
 */
@Entity
@Table(name = "advisor_requests")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdvisorRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnoreProperties({"advisor", "password", "hibernateLazyInitializer", "handler"})
    private User student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "faculty_id", nullable = false)
    @JsonIgnoreProperties({"advisor", "password", "hibernateLazyInitializer", "handler"})
    private User faculty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(length = 500)
    private String message;

    @Column(name = "response_remark", length = 500)
    private String responseRemark;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    public enum Status {
        PENDING,
        ACCEPTED,
        REJECTED
    }
}
