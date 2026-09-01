package com.edusync.dto;

import com.edusync.model.LeaveRequest;
import com.edusync.model.Notice;
import com.edusync.model.PlacementDrive;
import com.edusync.model.User;
import com.edusync.model.Assignment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// ─── Leave DTOs ─────────────────────────────────────────────────────────────

public class AppDto {

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LeaveRequestDto {
        @NotNull
        private LeaveRequest.LeaveType leaveType;
        @NotBlank
        private String reason;
        @NotNull
        private LocalDate fromDate;
        @NotNull
        private LocalDate toDate;
        private String eventName;
        private String attachmentPath;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LeaveApprovalDto {
        @NotNull
        private LeaveRequest.Status status;   // APPROVED or REJECTED
        private String remark;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UserSummary {
        private Long id;
        private String name;
        private String email;
        private String department;
        private String semester;
        private String section;
        private String rollNumber;
        private String employeeId;

        public static UserSummary from(User user) {
            if (user == null) return null;
            return UserSummary.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .department(user.getDepartment())
                    .semester(user.getSemester())
                    .section(user.getSection())
                    .rollNumber(user.getRollNumber())
                    .employeeId(user.getEmployeeId())
                    .build();
        }
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LeaveResponse {
        private Long id;
        private UserSummary student;
        private LeaveRequest.LeaveType leaveType;
        private String reason;
        private LocalDate fromDate;
        private LocalDate toDate;
        private int noOfDays;
        private String eventName;
        private String attachmentPath;
        private LeaveRequest.Status overallStatus;
        private LeaveRequest.Status facultyStatus;
        private String facultyRemark;
        private String facultyApprovedBy;
        private LocalDateTime facultyApprovedAt;
        private LeaveRequest.Status hodStatus;
        private String hodRemark;
        private String hodApprovedBy;
        private LocalDateTime hodApprovedAt;
        private LeaveRequest.Status principalStatus;
        private String principalRemark;
        private LocalDateTime principalApprovedAt;
        private String currentStage;
        private String pdfPath;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static LeaveResponse from(LeaveRequest leave) {
            if (leave == null) return null;
            return LeaveResponse.builder()
                    .id(leave.getId())
                    .student(UserSummary.from(leave.getStudent()))
                    .leaveType(leave.getLeaveType())
                    .reason(leave.getReason())
                    .fromDate(leave.getFromDate())
                    .toDate(leave.getToDate())
                    .noOfDays(leave.getNoOfDays())
                    .eventName(leave.getEventName())
                    .attachmentPath(leave.getAttachmentPath())
                    .overallStatus(leave.getOverallStatus())
                    .facultyStatus(leave.getFacultyStatus())
                    .facultyRemark(leave.getFacultyRemark())
                    .facultyApprovedBy(leave.getFacultyApprovedBy())
                    .facultyApprovedAt(leave.getFacultyApprovedAt())
                    .hodStatus(leave.getHodStatus())
                    .hodRemark(leave.getHodRemark())
                    .hodApprovedBy(leave.getHodApprovedBy())
                    .hodApprovedAt(leave.getHodApprovedAt())
                    .principalStatus(leave.getPrincipalStatus())
                    .principalRemark(leave.getPrincipalRemark())
                    .principalApprovedAt(leave.getPrincipalApprovedAt())
                    .currentStage(leave.getCurrentStage())
                    .pdfPath(leave.getPdfPath())
                    .createdAt(leave.getCreatedAt())
                    .updatedAt(leave.getUpdatedAt())
                    .build();
        }
    }

    // ─── Notice DTOs ────────────────────────────────────────────────────────

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class NoticeDto {
        @NotBlank
        private String title;
        @NotBlank
        private String content;
        private Notice.Category category;
        private Notice.Visibility visibility;
        private LocalDateTime expiresAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class NoticeResponse {
        private Long id;
        private String title;
        private String content;
        private String category;
        private String visibility;
        private String createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AssignmentDto {
        @NotBlank
        private String subject;
        @NotBlank
        private String title;
        @NotBlank
        private String description;
        private String semester;
        private Integer maxMarks;
        @NotNull
        private LocalDate dueDate;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AssignmentResponse {
        private Long id;
        private String subject;
        private String title;
        private String description;
        private String department;
        private String semester;
        private Integer maxMarks;
        private LocalDate dueDate;
        private UserSummary createdBy;
        private int recipientCount;
        private boolean active;
        private LocalDateTime createdAt;

        public static AssignmentResponse from(Assignment assignment, int recipientCount) {
            if (assignment == null) return null;
            return AssignmentResponse.builder()
                    .id(assignment.getId())
                    .subject(assignment.getSubject())
                    .title(assignment.getTitle())
                    .description(assignment.getDescription())
                    .department(assignment.getDepartment())
                    .semester(assignment.getSemester())
                    .maxMarks(assignment.getMaxMarks())
                    .dueDate(assignment.getDueDate())
                    .createdBy(UserSummary.from(assignment.getCreatedBy()))
                    .recipientCount(recipientCount)
                    .active(assignment.isActive())
                    .createdAt(assignment.getCreatedAt())
                    .build();
        }
    }

    // ─── Placement DTOs ─────────────────────────────────────────────────────

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PlacementDriveDto {
        @NotBlank
        private String companyName;
        private String logo;
        @NotBlank
        private String role;
        private String description;
        private Double packageLpa;
        private LocalDate driveDate;
        private LocalDate lastDateToApply;
        private Double minCgpa;
        private String eligibleBranches;
        private String eligibleBatch;
        private String jobType;
        private String location;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PlacementApplicationDto {
        private Long driveId;
        private Double cgpa;
    }

    // ─── Attendance DTOs ────────────────────────────────────────────────────

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AttendanceMark {
        @NotNull
        private Long studentId;
        @NotNull
        private Long subjectId;
        @NotNull
        private LocalDate date;
        @NotNull
        private com.edusync.model.Attendance.AttendanceStatus status;
        private Integer periodNumber;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AttendanceSummary {
        private String subjectName;
        private String subjectCode;
        private long totalClasses;
        private long attended;
        private double percentage;
    }

    // ─── Mark DTOs ──────────────────────────────────────────────────────────

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MarkDto {
        @NotNull
        private Long studentId;
        @NotNull
        private Long subjectId;
        @NotNull
        private com.edusync.model.Mark.ExamType examType;
        @NotNull
        private Double marksObtained;
        @NotNull
        private Double maxMarks;
        private String semester;
        private String academicYear;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BatchInternalMarkDto {
        private String seriesName;       // "Series I", "Series II", "Model Exam"
        private String subjectName;
        private String department;
        private String semester;
        private List<InternalMarkItem> marks;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class InternalMarkItem {
        private Long studentId;
        private Double testMarks;
        private Double assignmentMarks;
        private Double practicalMarks;
    }

    // ─── Grievance DTOs ─────────────────────────────────────────────────────

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class GrievanceDto {
        @NotNull
        private com.edusync.model.Grievance.Category category;
        @NotBlank
        private String subject;
        @NotBlank
        private String description;
        @NotNull
        private com.edusync.model.Grievance.TargetRole targetRole;
        private boolean anonymous;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class GrievanceReplyDto {
        @NotNull
        private com.edusync.model.Grievance.Status status;
        @NotBlank
        private String response;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class GrievanceResponse {
        private Long id;
        private UserSummary student;
        private String category;
        private String subject;
        private String description;
        private String targetRole;
        private String department;
        private String status;
        private boolean anonymous;
        private String response;
        private String respondedBy;
        private LocalDateTime respondedAt;
        private LocalDateTime createdAt;

        public static GrievanceResponse from(com.edusync.model.Grievance g) {
            if (g == null) return null;
            UserSummary sSummary = g.isAnonymous() ? null : UserSummary.from(g.getStudent());
            return GrievanceResponse.builder()
                    .id(g.getId())
                    .student(sSummary)
                    .category(g.getCategory().name())
                    .subject(g.getSubject())
                    .description(g.getDescription())
                    .targetRole(g.getTargetRole().name())
                    .department(g.getDepartment())
                    .status(g.getStatus().name())
                    .anonymous(g.isAnonymous())
                    .response(g.getResponse())
                    .respondedBy(g.getRespondedBy())
                    .respondedAt(g.getRespondedAt())
                    .createdAt(g.getCreatedAt())
                    .build();
        }
    }

    // ─── Dashboard stats ────────────────────────────────────────────────────

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DashboardStats {
        private long totalStudents;
        private long totalFaculty;
        private long pendingLeaves;
        private long activeNotices;
        private long upcomingDrives;
        private long totalDepartments;
    }

    // ─── Generic API response wrapper ───────────────────────────────────────

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> ApiResponse<T> ok(String msg, T data) {
            return AppDto.ApiResponse.<T>builder()
                    .success(true).message(msg).data(data).build();
        }

        public static <T> ApiResponse<T> error(String msg) {
            return AppDto.ApiResponse.<T>builder()
                    .success(false).message(msg).build();
        }
    }
}
