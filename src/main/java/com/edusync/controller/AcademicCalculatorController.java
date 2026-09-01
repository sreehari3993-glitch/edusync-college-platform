package com.edusync.controller;

import com.edusync.dto.AppDto;
import com.edusync.security.JwtUtil;
import com.edusync.service.AcademicCalculatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for KTU Semester Grade Predictor and Attendance What-If Simulators.
 */
@RestController
@RequestMapping("/api/calculator")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class AcademicCalculatorController {

    private final AcademicCalculatorService calculatorService;
    private final JwtUtil jwtUtil;

    /**
     * Solves required ESE marks (out of 100) for a target grade.
     */
    @PostMapping("/grade-predict")
    public ResponseEntity<AppDto.ApiResponse<AcademicCalculatorService.GradePredictionResult>> predictGrade(
            @RequestBody Map<String, Object> body
    ) {
        String subject = (String) body.getOrDefault("subjectName", "Course");
        double internalMarks = 40.0;
        if (body.get("internalMarks") != null) {
            internalMarks = Double.parseDouble(body.get("internalMarks").toString());
        }
        String targetGrade = (String) body.getOrDefault("targetGrade", "A");

        var result = calculatorService.predictEseRequired(subject, internalMarks, targetGrade);
        return ResponseEntity.ok(AppDto.ApiResponse.ok(result));
    }

    /**
     * Simulates attendance change when missing or attending classes.
     */
    @PostMapping("/attendance-simulate")
    public ResponseEntity<AppDto.ApiResponse<AcademicCalculatorService.AttendanceSimulationResult>> simulateAttendance(
            @RequestBody Map<String, Object> body
    ) {
        String subject = (String) body.getOrDefault("subjectName", "Selected Subject");
        long currentPresent = Long.parseLong(body.getOrDefault("currentPresent", 25).toString());
        long currentTotal = Long.parseLong(body.getOrDefault("currentTotal", 30).toString());
        long missed = Long.parseLong(body.getOrDefault("additionalMissed", 0).toString());
        long attended = Long.parseLong(body.getOrDefault("additionalAttended", 0).toString());

        var result = calculatorService.simulateAttendance(subject, currentPresent, currentTotal, missed, attended);
        return ResponseEntity.ok(AppDto.ApiResponse.ok(result));
    }

    /**
     * Returns pre-populated enrolled academic overview for the student.
     */
    @GetMapping("/student-summary")
    public ResponseEntity<AppDto.ApiResponse<AcademicCalculatorService.StudentAcademicOverview>> getStudentOverview(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) Long studentId
    ) {
        Long resolvedId = studentId;
        if (resolvedId == null && authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                resolvedId = jwtUtil.extractUserId(authHeader.substring(7));
            } catch (Exception ignored) {}
        }
        if (resolvedId == null) {
            resolvedId = 1L; // fallback default demo student
        }

        try {
            var overview = calculatorService.getStudentAcademicOverview(resolvedId);
            return ResponseEntity.ok(AppDto.ApiResponse.ok(overview));
        } catch (Exception e) {
            log.warn("Could not fetch full academic overview for student {}: {}", resolvedId, e.getMessage());
            return ResponseEntity.badRequest().body(AppDto.ApiResponse.error(e.getMessage()));
        }
    }
}
