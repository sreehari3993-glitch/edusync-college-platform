package com.edusync.controller;

import com.edusync.dto.AppDto;
import com.edusync.model.LeaveRequest;
import com.edusync.security.JwtUtil;
import com.edusync.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Leave request REST controller.
 *
 * POST   /api/leaves/submit                    — student submits leave
 * GET    /api/leaves/my                        — student views own leaves
 * GET    /api/leaves/faculty/pending           — faculty views pending in their dept
 * PUT    /api/leaves/{id}/faculty-action       — faculty approves/rejects
 * GET    /api/leaves/hod/pending               — HOD views pending
 * PUT    /api/leaves/{id}/hod-action           — HOD approves/rejects
 * GET    /api/leaves/principal/pending         — principal views pending
 * PUT    /api/leaves/{id}/principal-action     — principal approves/rejects
 * GET    /api/leaves/all                       — admin views all
 */
@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
@CrossOrigin
public class LeaveController {

    private final LeaveService leaveService;
    private final JwtUtil jwtUtil;

    // ─── Student endpoints ────────────────────────────────────────────────

    @PostMapping("/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AppDto.ApiResponse<AppDto.LeaveResponse>> submitLeave(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody AppDto.LeaveRequestDto dto
    ) {
        Long studentId = extractUserId(authHeader);
        LeaveRequest leave = leaveService.submitLeave(studentId, dto);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Leave submitted successfully", AppDto.LeaveResponse.from(leave)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AppDto.ApiResponse<List<AppDto.LeaveResponse>>> getMyLeaves(
            @RequestHeader("Authorization") String authHeader
    ) {
        Long studentId = extractUserId(authHeader);
        List<LeaveRequest> leaves = leaveService.getStudentLeaves(studentId);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Fetched your leave requests", toLeaveResponses(leaves)));
    }

    // ─── Faculty endpoints ────────────────────────────────────────────────

    @GetMapping("/faculty/pending")
    @PreAuthorize("hasAnyRole('FACULTY','HOD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<List<AppDto.LeaveResponse>>> pendingForFaculty(
            @RequestParam String department
    ) {
        List<LeaveRequest> list = leaveService.getPendingForFaculty(department);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Pending faculty review", toLeaveResponses(list)));
    }

    @GetMapping("/faculty/processed")
    @PreAuthorize("hasAnyRole('FACULTY','HOD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<List<AppDto.LeaveResponse>>> processedForFaculty(
            @RequestParam String department
    ) {
        List<LeaveRequest> list = leaveService.getProcessedForFaculty(department);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Processed faculty reviews", toLeaveResponses(list)));
    }

    @PutMapping("/{id}/faculty-action")
    @PreAuthorize("hasAnyRole('FACULTY','HOD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<AppDto.LeaveResponse>> facultyAction(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody AppDto.LeaveApprovalDto dto
    ) {
        Long facultyId = extractUserId(authHeader);
        LeaveRequest updated = leaveService.facultyAction(id, facultyId, dto);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Action recorded", AppDto.LeaveResponse.from(updated)));
    }

    // ─── HOD endpoints ────────────────────────────────────────────────────

    @GetMapping("/hod/pending")
    @PreAuthorize("hasAnyRole('HOD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<List<AppDto.LeaveResponse>>> pendingForHod(
            @RequestParam String department
    ) {
        List<LeaveRequest> list = leaveService.getPendingForHod(department);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Pending HOD review", toLeaveResponses(list)));
    }

    @PutMapping("/{id}/hod-action")
    @PreAuthorize("hasAnyRole('HOD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<AppDto.LeaveResponse>> hodAction(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody AppDto.LeaveApprovalDto dto
    ) {
        Long hodId = extractUserId(authHeader);
        LeaveRequest updated = leaveService.hodAction(id, hodId, dto);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("HOD action recorded", AppDto.LeaveResponse.from(updated)));
    }

    // ─── Principal endpoints ──────────────────────────────────────────────

    @GetMapping("/principal/pending")
    @PreAuthorize("hasAnyRole('PRINCIPAL','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<List<AppDto.LeaveResponse>>> pendingForPrincipal() {
        List<LeaveRequest> list = leaveService.getPendingForPrincipal();
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Pending principal review", toLeaveResponses(list)));
    }

    @PutMapping("/{id}/principal-action")
    @PreAuthorize("hasAnyRole('PRINCIPAL','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<AppDto.LeaveResponse>> principalAction(
            @PathVariable Long id,
            @RequestBody AppDto.LeaveApprovalDto dto
    ) {
        LeaveRequest updated = leaveService.principalAction(id, dto);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Principal action recorded", AppDto.LeaveResponse.from(updated)));
    }

    // ─── Admin endpoints ──────────────────────────────────────────────────

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<List<AppDto.LeaveResponse>>> getAllLeaves() {
        List<LeaveRequest> list = leaveService.getAllLeaves();
        return ResponseEntity.ok(AppDto.ApiResponse.ok("All leave requests", toLeaveResponses(list)));
    }

    @GetMapping("/department")
    @PreAuthorize("hasAnyRole('HOD','ADMIN','PRINCIPAL')")
    public ResponseEntity<AppDto.ApiResponse<List<AppDto.LeaveResponse>>> getByDepartment(
            @RequestParam String dept
    ) {
        List<LeaveRequest> list = leaveService.getLeavesByDept(dept);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Department leaves", toLeaveResponses(list)));
    }

    // ─── PDF Certificate Endpoints ────────────────────────────────────────
    
    @GetMapping(value = "/{id}/pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> getLeaveCertificatePdf(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean download
    ) {
        byte[] pdf = leaveService.generateLeavePdf(id);
        String disposition = download ? "attachment; filename=\"Duty_Leave_Certificate_" + id + ".pdf\""
                                      : "inline; filename=\"Duty_Leave_Certificate_" + id + ".pdf\"";

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "application/pdf")
                .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(pdf);
    }

    // ─── Helper ───────────────────────────────────────────────────────────

    private Long extractUserId(String authHeader) {
        String token = authHeader.substring(7);
        return jwtUtil.extractUserId(token);
    }

    private List<AppDto.LeaveResponse> toLeaveResponses(List<LeaveRequest> leaves) {
        return leaves.stream()
                .map(AppDto.LeaveResponse::from)
                .toList();
    }
}
