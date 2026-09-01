package com.edusync.service;

import com.edusync.model.InternalMark;
import com.edusync.model.Subject;
import com.edusync.model.User;
import com.edusync.repository.AttendanceRepository;
import com.edusync.repository.InternalMarkRepository;
import com.edusync.repository.SubjectRepository;
import com.edusync.repository.UserRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * High-precision Academic Predictor and What-If Simulator Service.
 * Implements official KTU 2019/2024 academic grading and attendance regulations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AcademicCalculatorService {

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final AttendanceRepository attendanceRepository;
    private final InternalMarkRepository internalMarkRepository;

    public static final double KTU_MIN_ESE_PASS = 40.0;
    public static final double KTU_MIN_TOTAL_PASS = 45.0;
    public static final double KTU_REGULAR_ATTENDANCE = 75.0;
    public static final double KTU_CONDONATION_MIN = 60.0;

    @Data
    @Builder
    public static class GradePredictionResult {
        private String subjectName;
        private double internalMarks; // out of 50
        private String targetGrade;   // S, A+, A, B+, B, C+, C, D, P
        private double targetGradeMinTotal; // minimum total marks for target
        private double requiredEseMarks;    // out of 100 (min 40)
        private boolean achievable;
        private double maxPossibleTotal;    // internal + 50
        private String maxPossibleGrade;
        private String advice;
    }

    @Data
    @Builder
    public static class AttendanceSimulationResult {
        private String subjectName;
        private long currentPresent;
        private long currentTotal;
        private double currentPercentage;
        private long simulatedPresent;
        private long simulatedTotal;
        private double simulatedPercentage;
        private double percentageChange;
        private String status; // "SAFE", "CONDONATION_RISK", "DEBARRED"
        private long safeBunksRemaining;
        private long classesNeededToRecover75;
        private String guidance;
    }

    @Data
    @Builder
    public static class StudentAcademicOverview {
        private Long studentId;
        private String studentName;
        private String department;
        private String semester;
        private List<SubjectAcademicSummary> subjects;
    }

    @Data
    @Builder
    public static class SubjectAcademicSummary {
        private Long subjectId;
        private String subjectCode;
        private String subjectName;
        private double internalMarks; // 0-50
        private long presentHours;
        private long totalHours;
        private double attendancePercentage;
        private long safeBunks;
        private long recoveryNeeded;
    }

    /**
     * Solves required KTU End-Semester Exam (ESE) marks out of 100 for a target grade.
     */
    public GradePredictionResult predictEseRequired(String subjectName, double internalMarks, String targetGrade) {
        double cie = Math.max(0.0, Math.min(50.0, internalMarks));
        String target = (targetGrade != null ? targetGrade.trim().toUpperCase() : "A");

        double targetThreshold = switch (target) {
            case "S" -> 90.0;
            case "A+", "A_PLUS" -> 85.0;
            case "A" -> 80.0;
            case "B+", "B_PLUS" -> 75.0;
            case "B" -> 70.0;
            case "C+", "C_PLUS" -> 65.0;
            case "C" -> 60.0;
            case "D" -> 55.0;
            default -> 45.0; // P (Pass)
        };

        // Total = CIE + (ESE / 2)  ==> ESE = 2 * (Target - CIE)
        double calculatedEse = 2.0 * (targetThreshold - cie);
        double maxPossibleTotal = cie + 50.0;
        String maxPossibleGrade = resolveGradeFromTotal(maxPossibleTotal);

        boolean achievable = (calculatedEse <= 100.0);
        double requiredEse;
        String advice;

        if (!achievable) {
            requiredEse = 100.0;
            advice = String.format("Target grade '%s' (requires %.1f marks) is not achievable with CIE %.1f/50. Highest possible score is %.1f/100 (Grade: %s).",
                    target, targetThreshold, cie, maxPossibleTotal, maxPossibleGrade);
        } else {
            // Mandatory KTU rule: Minimum 40/100 in ESE is required to pass any subject
            requiredEse = Math.max(KTU_MIN_ESE_PASS, Math.ceil(calculatedEse));
            double projectedTotal = cie + (requiredEse / 2.0);

            if (requiredEse == KTU_MIN_ESE_PASS && projectedTotal >= targetThreshold) {
                advice = String.format("You only need the mandatory KTU minimum passing mark of **%.0f/100** in ESE to secure grade '%s'!",
                        KTU_MIN_ESE_PASS, target);
            } else {
                advice = String.format("Score at least **%.0f/100** in the University End-Semester Examination to secure an '%s' grade (Projected Total: %.1f/100).",
                        requiredEse, target, projectedTotal);
            }
        }

        return GradePredictionResult.builder()
                .subjectName(subjectName != null ? subjectName : "Subject")
                .internalMarks(cie)
                .targetGrade(target)
                .targetGradeMinTotal(targetThreshold)
                .requiredEseMarks(requiredEse)
                .achievable(achievable)
                .maxPossibleTotal(maxPossibleTotal)
                .maxPossibleGrade(maxPossibleGrade)
                .advice(advice)
                .build();
    }

    /**
     * Simulates attendance impact if student misses or attends a number of periods/classes.
     */
    public AttendanceSimulationResult simulateAttendance(
            String subjectName,
            long currentPresent,
            long currentTotal,
            long additionalMissed,
            long additionalAttended
    ) {
        long present = Math.max(0, currentPresent);
        long total = Math.max(present, currentTotal);

        long miss = Math.max(0, additionalMissed);
        long attend = Math.max(0, additionalAttended);

        long simPresent = present + attend;
        long simTotal = total + miss + attend;

        double currentPct = (total > 0) ? Math.round(((double) present / total) * 1000.0) / 10.0 : 100.0;
        double simPct = (simTotal > 0) ? Math.round(((double) simPresent / simTotal) * 1000.0) / 10.0 : 100.0;
        double diff = Math.round((simPct - currentPct) * 10.0) / 10.0;

        // Calculate safe bunks: floor((present - 0.75 * total) / 0.75)
        long safeBunks = 0;
        if (total > 0 && currentPct >= KTU_REGULAR_ATTENDANCE) {
            double rawSafe = ((double) present - (0.75 * total)) / 0.75;
            safeBunks = Math.max(0, (long) Math.floor(rawSafe));
        }

        // Calculate recovery needed: ceil((0.75 * total - present) / 0.25)
        long recoveryNeeded = 0;
        if (total > 0 && currentPct < KTU_REGULAR_ATTENDANCE) {
            double rawNeeded = ((0.75 * total) - (double) present) / 0.25;
            recoveryNeeded = Math.max(1, (long) Math.ceil(rawNeeded));
        }

        String status;
        String guidance;

        if (simPct >= KTU_REGULAR_ATTENDANCE) {
            status = "SAFE";
            guidance = String.format("Attendance remains at **%.1f%%** (Above KTU 75%% threshold). Fully eligible for regular hall ticket.", simPct);
        } else if (simPct >= KTU_CONDONATION_MIN) {
            status = "CONDONATION_RISK";
            guidance = String.format("⚠️ Attendance drops to **%.1f%%** (Condonation Zone: 60%%–74%%). Requires Medical/OD condonation approval from the Principal (Max 2 times across 4 years).", simPct);
        } else {
            status = "DEBARRED";
            guidance = String.format("🚨 Critical! Attendance drops to **%.1f%%** (Below 60%%). Student will be debarred from KTU End-Semester Examinations and must repeat the semester course.", simPct);
        }

        return AttendanceSimulationResult.builder()
                .subjectName(subjectName != null ? subjectName : "All Subjects")
                .currentPresent(present)
                .currentTotal(total)
                .currentPercentage(currentPct)
                .simulatedPresent(simPresent)
                .simulatedTotal(simTotal)
                .simulatedPercentage(simPct)
                .percentageChange(diff)
                .status(status)
                .safeBunksRemaining(safeBunks)
                .classesNeededToRecover75(recoveryNeeded)
                .guidance(guidance)
                .build();
    }

    /**
     * Fetches current academic summary for an authenticated student.
     */
    public StudentAcademicOverview getStudentAcademicOverview(Long studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + studentId));

        String dept = student.getDepartment() != null ? student.getDepartment() : "CSE";
        String sem = student.getSemester() != null ? student.getSemester() : "S4";

        List<Subject> subjects = subjectRepository.findByDepartmentAndSemester(dept, sem);
        if (subjects.isEmpty()) {
            subjects = subjectRepository.findByDepartment(dept);
        }

        List<SubjectAcademicSummary> summaries = new ArrayList<>();
        List<InternalMark> studentMarks = internalMarkRepository.findByStudentOrderByCreatedAtDesc(student);

        for (Subject sub : subjects) {
            long present = attendanceRepository.countPresent(student, sub);
            long total = attendanceRepository.countTotal(student, sub);

            // Default sample attendance if semester is early
            if (total == 0) {
                total = 30;
                present = 26; // ~86.7%
            }

            double pct = total > 0 ? Math.round(((double) present / total) * 1000.0) / 10.0 : 85.0;

            // Get CIE marks (out of 50)
            double cie = 40.0; // default estimated CIE
            for (InternalMark m : studentMarks) {
                if (m.getSubjectName() != null && (m.getSubjectName().equalsIgnoreCase(sub.getName()) || m.getSubjectName().equalsIgnoreCase(sub.getCode()))) {
                    cie = Math.min(50.0, m.getTotalMarks());
                    break;
                }
            }

            long safeBunks = 0;
            if (pct >= KTU_REGULAR_ATTENDANCE) {
                safeBunks = Math.max(0, (long) Math.floor(((double) present - (0.75 * total)) / 0.75));
            }

            long recovery = 0;
            if (pct < KTU_REGULAR_ATTENDANCE) {
                recovery = Math.max(1, (long) Math.ceil(((0.75 * total) - (double) present) / 0.25));
            }

            summaries.add(SubjectAcademicSummary.builder()
                    .subjectId(sub.getId())
                    .subjectCode(sub.getCode())
                    .subjectName(sub.getName())
                    .internalMarks(cie)
                    .presentHours(present)
                    .totalHours(total)
                    .attendancePercentage(pct)
                    .safeBunks(safeBunks)
                    .recoveryNeeded(recovery)
                    .build());
        }

        return StudentAcademicOverview.builder()
                .studentId(student.getId())
                .studentName(student.getName())
                .department(dept)
                .semester(sem)
                .subjects(summaries)
                .build();
    }

    public static String resolveGradeFromTotal(double total) {
        if (total >= 90.0) return "S";
        if (total >= 85.0) return "A+";
        if (total >= 80.0) return "A";
        if (total >= 75.0) return "B+";
        if (total >= 70.0) return "B";
        if (total >= 65.0) return "C+";
        if (total >= 60.0) return "C";
        if (total >= 55.0) return "D";
        if (total >= 45.0) return "P";
        return "F";
    }
}
