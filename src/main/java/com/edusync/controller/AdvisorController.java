package com.edusync.controller;

import com.edusync.dto.AdvisorDto;
import com.edusync.dto.AppDto;
import com.edusync.security.JwtUtil;
import com.edusync.service.AdvisorDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for Class Advisor Mentorship Hub & Early-Warning Monitoring.
 */
@RestController
@RequestMapping("/api/advisor")
@RequiredArgsConstructor
@CrossOrigin
public class AdvisorController {

    private final AdvisorDashboardService advisorDashboardService;
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
}
