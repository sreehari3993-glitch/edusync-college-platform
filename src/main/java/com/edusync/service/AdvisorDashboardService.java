package com.edusync.service;

import com.edusync.dto.AdvisorDto;
import com.edusync.dto.NotificationDto;
import com.edusync.exception.ResourceNotFoundException;
import com.edusync.model.Attendance;
import com.edusync.model.InternalMark;
import com.edusync.model.User;
import com.edusync.repository.AttendanceRepository;
import com.edusync.repository.InternalMarkRepository;
import com.edusync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for Class Advisor Mentorship Command Center & Early Warning System.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdvisorDashboardService {

    private final UserRepository userRepo;
    private final AttendanceRepository attendanceRepo;
    private final InternalMarkRepository internalMarkRepo;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public AdvisorDto.AdvisorBatchOverviewDto getAdvisorDashboard(Long facultyId) {
        User advisor = userRepo.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty", facultyId));

        // 1. Fetch assigned mentees, fallback to department students if none assigned yet
        List<User> students = userRepo.findByAdvisorAndActiveTrue(advisor);
        if (students.isEmpty() && advisor.getDepartment() != null) {
            students = userRepo.findByDepartmentAndRole(advisor.getDepartment(), User.Role.STUDENT);
        }

        List<AdvisorDto.StudentMenteeRiskDto> riskList = new ArrayList<>();
        int criticalCount = 0;
        int warningCount = 0;
        int safeCount = 0;

        for (User student : students) {
            if (!student.isActive()) continue;

            AdvisorDto.StudentMenteeRiskDto riskDto = calculateStudentRisk(student);
            riskList.add(riskDto);

            if (riskDto.getOverallRisk() == AdvisorDto.RiskLevel.CRITICAL) {
                criticalCount++;
            } else if (riskDto.getOverallRisk() == AdvisorDto.RiskLevel.WARNING) {
                warningCount++;
            } else {
                safeCount++;
            }
        }

        // Sort critical risks first, then warnings, then safe
        riskList.sort((a, b) -> a.getOverallRisk().compareTo(b.getOverallRisk()));

        return AdvisorDto.AdvisorBatchOverviewDto.builder()
                .advisorId(advisor.getId())
                .advisorName(advisor.getName())
                .department(advisor.getDepartment())
                .totalMentees(riskList.size())
                .criticalCount(criticalCount)
                .warningCount(warningCount)
                .safeCount(safeCount)
                .students(riskList)
                .build();
    }

    /**
     * Calculates attendance and series exam academic risks for a student based on KTU criteria.
     */
    public AdvisorDto.StudentMenteeRiskDto calculateStudentRisk(User student) {
        List<String> riskFactors = new ArrayList<>();

        // ─── 1. Attendance Calculation ───
        List<Attendance> attendanceRecords = attendanceRepo.findByStudent(student);
        long totalClasses = attendanceRecords.size();
        long attendedClasses = attendanceRecords.stream()
                .filter(a -> a.getStatus() == Attendance.AttendanceStatus.PRESENT
                        || a.getStatus() == Attendance.AttendanceStatus.OD)
                .count();

        double attendancePct = totalClasses > 0
                ? Math.round((attendedClasses * 100.0 / totalClasses) * 10.0) / 10.0
                : 82.5; // default fallback if fresh record

        AdvisorDto.RiskLevel attendanceRisk;
        if (attendancePct < 75.0) {
            attendanceRisk = AdvisorDto.RiskLevel.CRITICAL;
            riskFactors.add("Attendance " + attendancePct + "% is below the 75% KTU condonation limit.");
        } else if (attendancePct < 80.0) {
            attendanceRisk = AdvisorDto.RiskLevel.WARNING;
            riskFactors.add("Attendance " + attendancePct + "% is in the 75%-80% borderline range.");
        } else {
            attendanceRisk = AdvisorDto.RiskLevel.SAFE;
        }

        // ─── 2. Internal Series Exam Marks Calculation ───
        List<InternalMark> internalMarks = internalMarkRepo.findByStudentOrderByCreatedAtDesc(student);
        double seriesAvgPct;
        if (!internalMarks.isEmpty()) {
            double totalPctSum = 0.0;
            for (InternalMark mark : internalMarks) {
                double max = mark.getMaxMarks() != null && mark.getMaxMarks() > 0 ? mark.getMaxMarks() : 50.0;
                double obtained = mark.getTotalMarks() != null ? mark.getTotalMarks() : 0.0;
                totalPctSum += (obtained / max) * 100.0;
            }
            seriesAvgPct = Math.round((totalPctSum / internalMarks.size()) * 10.0) / 10.0;
        } else {
            seriesAvgPct = 68.0; // default baseline for demo if no test marks recorded yet
        }

        AdvisorDto.RiskLevel seriesMarksRisk;
        if (seriesAvgPct < 40.0) {
            seriesMarksRisk = AdvisorDto.RiskLevel.CRITICAL;
            riskFactors.add("Series Exam Avg " + seriesAvgPct + "% is below the 40% minimum passing requirement.");
        } else if (seriesAvgPct < 50.0) {
            seriesMarksRisk = AdvisorDto.RiskLevel.WARNING;
            riskFactors.add("Series Exam Avg " + seriesAvgPct + "% is marginal (40%-50%).");
        } else {
            seriesMarksRisk = AdvisorDto.RiskLevel.SAFE;
        }

        // ─── 3. Composite Risk Evaluation ───
        AdvisorDto.RiskLevel overallRisk;
        if (attendanceRisk == AdvisorDto.RiskLevel.CRITICAL || seriesMarksRisk == AdvisorDto.RiskLevel.CRITICAL) {
            overallRisk = AdvisorDto.RiskLevel.CRITICAL;
        } else if (attendanceRisk == AdvisorDto.RiskLevel.WARNING || seriesMarksRisk == AdvisorDto.RiskLevel.WARNING) {
            overallRisk = AdvisorDto.RiskLevel.WARNING;
        } else {
            overallRisk = AdvisorDto.RiskLevel.SAFE;
        }

        return AdvisorDto.StudentMenteeRiskDto.builder()
                .studentId(student.getId())
                .name(student.getName())
                .email(student.getEmail())
                .rollNumber(student.getRollNumber() != null ? student.getRollNumber() : "TKM-STU-" + student.getId())
                .department(student.getDepartment())
                .semester(student.getSemester() != null ? student.getSemester() : "S2")
                .section(student.getSection() != null ? student.getSection() : "A")
                .phone(student.getPhone() != null ? student.getPhone() : "+91 98765 43210")
                .attendancePercentage(attendancePct)
                .totalClasses(totalClasses)
                .attendedClasses(attendedClasses)
                .attendanceRisk(attendanceRisk)
                .seriesExamAvgPercentage(seriesAvgPct)
                .seriesRecordsCount(internalMarks.size())
                .seriesMarksRisk(seriesMarksRisk)
                .overallRisk(overallRisk)
                .riskFactors(riskFactors)
                .build();
    }

    /**
     * Dispatches parent/student counseling alert with real-time push notification.
     */
    @Transactional
    public void sendMenteeAlert(Long facultyId, AdvisorDto.MenteeAlertRequest req) {
        User advisor = userRepo.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty", facultyId));
        User student = userRepo.findById(req.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", req.getStudentId()));

        String channel = req.getChannel() != null ? req.getChannel() : "IN_APP";
        String message = req.getMessage() != null && !req.getMessage().isBlank()
                ? req.getMessage()
                : "Academic Alert: Class Advisor " + advisor.getName() + " has flagged an attendance/marks review for you. Please meet during advisory hours.";

        // Send real-time SSE push notification to student
        notificationService.sendToUser(
                student.getId(),
                NotificationDto.builder()
                        .type("ADVISOR_ALERT")
                        .title("👨‍🏫 Advisory Alert from " + advisor.getName())
                        .message(message)
                        .actorName(advisor.getName())
                        .department(student.getDepartment())
                        .link("#page-student")
                        .build()
        );

        auditService.log(
                "ADVISORY_ALERT_DISPATCHED",
                advisor.getEmail(),
                advisor.getName(),
                advisor.getRole(),
                "User",
                student.getId().toString(),
                "Sent " + channel + " advisory alert to " + student.getName() + " (" + student.getEmail() + ")"
        );
    }
}
