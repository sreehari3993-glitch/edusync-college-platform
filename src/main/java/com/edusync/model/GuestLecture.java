package com.edusync.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Guest Lecture & Industry Expert Session entity.
 */
@Entity
@Table(name = "guest_lectures")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuestLecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String speaker;

    @Column
    private String designation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "company_name")
    private String companyName;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    private LocalDate date;

    @Column
    private String time;

    @Column
    private String venue;

    @Column
    private String departments;       // comma-separated: "CSE,ECE"

    @Column
    private Integer attendance;

    @Column(name = "feedback_avg")
    private Double feedbackAvg;       // e.g. 4.6 out of 5.0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coordinator_id")
    private User coordinator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LectureStatus status = LectureStatus.UPCOMING;

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

    public enum LectureStatus {
        UPCOMING,
        COMPLETED,
        CANCELLED
    }
}
