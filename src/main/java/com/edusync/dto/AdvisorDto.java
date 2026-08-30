package com.edusync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class AdvisorDto {

    public enum RiskLevel {
        CRITICAL,  // 🔴 Attendance < 75% OR Series Marks < 40%
        WARNING,   // 🟡 Attendance 75-80% OR Series Marks 40-50%
        SAFE       // 🟢 Attendance >= 80% AND Series Marks >= 50%
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StudentMenteeRiskDto {
        private Long studentId;
        private String name;
        private String email;
        private String rollNumber;
        private String department;
        private String semester;
        private String section;
        private String phone;

        // Attendance Stats
        private double attendancePercentage;
        private long totalClasses;
        private long attendedClasses;
        private RiskLevel attendanceRisk;

        // Series Exam Marks Stats
        private double seriesExamAvgPercentage;
        private int seriesRecordsCount;
        private RiskLevel seriesMarksRisk;

        // Composite Risk
        private RiskLevel overallRisk;
        private List<String> riskFactors;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdvisorBatchOverviewDto {
        private Long advisorId;
        private String advisorName;
        private String department;
        private int totalMentees;
        private int criticalCount;
        private int warningCount;
        private int safeCount;
        private List<StudentMenteeRiskDto> students;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenteeAlertRequest {
        private Long studentId;
        private String channel; // IN_APP, WHATSAPP, EMAIL
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdvisorRequestSubmitDto {
        private Long facultyId;
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdvisorRequestActionDto {
        private String action; // ACCEPT, REJECT
        private String remark;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StudentAdvisorStatusDto {
        private Long advisorId;
        private String advisorName;
        private String advisorEmail;
        private String advisorDepartment;
        private String advisorPhone;
        private com.edusync.model.AdvisorRequest latestRequest;
    }
}
