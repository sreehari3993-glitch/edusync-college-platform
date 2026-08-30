package com.edusync.controller;

import com.edusync.dto.AdvisorDto;
import com.edusync.dto.AppDto;
import com.edusync.model.AdvisorRequest;
import com.edusync.security.JwtUtil;
import com.edusync.service.AdvisorDashboardService;
import com.edusync.service.AdvisorRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for Class Advisor Mentorship Hub & Student Advisor Requests.
 */
@RestController
@RequestMapping("/api/advisor")
@RequiredArgsConstructor
@CrossOrigin
public class AdvisorController {

    private final AdvisorDashboardService advisorDashboardService;
    private final AdvisorRequestService advisorRequestService;
    private final com.edusync.service.EmailService emailService;
    private final JwtUtil jwtUtil;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('FACULTY','HOD','PRINCIPAL','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<AdvisorDto.AdvisorBatchOverviewDto>> getAdvisorDashboard(
            @RequestHeader("Authorization") String authHeader
    ) {
        Long facultyId = jwtUtil.extractUserId(authHeader.substring(7));
        AdvisorDto.AdvisorBatchOverviewDto data = advisorDashboardService.getAdvisorDashboard(facultyId);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Advisor mentorship dashboard loaded", data));
    }

    @PostMapping("/alert")
    @PreAuthorize("hasAnyRole('FACULTY','HOD','PRINCIPAL','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<Void>> sendMenteeAlert(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody AdvisorDto.MenteeAlertRequest request
    ) {
        Long facultyId = jwtUtil.extractUserId(authHeader.substring(7));
        advisorDashboardService.sendMenteeAlert(facultyId, request);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Advisory alert dispatched successfully", null));
    }

    @PostMapping("/test-email")
    public ResponseEntity<AppDto.ApiResponse<String>> sendTestEmail(
            @RequestParam String to
    ) {
        emailService.sendParentAcademicAlertEmail(
                to,
                "Anzal Rahman",
                "TKM22CS001",
                "CSE",
                68.5,
                35.0,
                "Prof. Meera Nair",
                "This is a verified live academic advisory notice from EduSync TKMIT Academic Portal."
        );
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Live test email dispatched to " + to, "OK"));
    }

    // ─── Student Advisor Request Endpoints ──────────────────────────────

    @PostMapping("/requests/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AppDto.ApiResponse<AdvisorRequest>> submitAdvisorRequest(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody AdvisorDto.AdvisorRequestSubmitDto request
    ) {
        Long studentId = jwtUtil.extractUserId(authHeader.substring(7));
        AdvisorRequest result = advisorRequestService.submitAdvisorRequest(studentId, request);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Advisor request submitted successfully", result));
    }

    @GetMapping("/requests/student-status")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AppDto.ApiResponse<AdvisorDto.StudentAdvisorStatusDto>> getStudentAdvisorStatus(
            @RequestHeader("Authorization") String authHeader
    ) {
        Long studentId = jwtUtil.extractUserId(authHeader.substring(7));
        AdvisorDto.StudentAdvisorStatusDto status = advisorRequestService.getStudentAdvisorStatus(studentId);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Student advisor status retrieved", status));
    }

    @GetMapping("/requests/faculty-list")
    @PreAuthorize("hasAnyRole('FACULTY','HOD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<List<AdvisorRequest>>> getFacultyAdvisorRequests(
            @RequestHeader("Authorization") String authHeader
    ) {
        Long facultyId = jwtUtil.extractUserId(authHeader.substring(7));
        List<AdvisorRequest> list = advisorRequestService.getFacultyAdvisorRequests(facultyId);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Faculty advisor requests retrieved", list));
    }

    @PutMapping("/requests/{id}/respond")
    @PreAuthorize("hasAnyRole('FACULTY','HOD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<AdvisorRequest>> respondToAdvisorRequest(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody AdvisorDto.AdvisorRequestActionDto actionDto
    ) {
        Long facultyId = jwtUtil.extractUserId(authHeader.substring(7));
        AdvisorRequest updated = advisorRequestService.respondToRequest(facultyId, id, actionDto);
        String actionText = updated.getStatus() == AdvisorRequest.Status.ACCEPTED ? "accepted" : "declined";
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Advisor request " + actionText + " successfully", updated));
    }
}
