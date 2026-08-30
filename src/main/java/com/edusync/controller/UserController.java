package com.edusync.controller;

import com.edusync.dto.AdvisorDto;
import com.edusync.dto.AppDto;
import com.edusync.dto.AuthDto;
import com.edusync.model.AdvisorRequest;
import com.edusync.model.User;
import com.edusync.security.JwtUtil;
import com.edusync.service.AdvisorRequestService;
import com.edusync.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User / Profile controller.
 *
 * GET  /api/users/profile         — own profile (any authenticated user)
 * GET  /api/users/admin/all       — admin: all users
 * GET  /api/users/admin/stats     — admin: dashboard statistics
 * PUT  /api/users/admin/{id}/toggle — admin: activate/deactivate
 * GET  /api/users/by-role?role=   — filter by role
 * GET  /api/users/by-dept?dept=   — filter by department
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin
public class UserController {

    private final UserService userService;
    private final AdvisorRequestService advisorRequestService;
    private final JwtUtil jwtUtil;

    @GetMapping("/profile")
    public ResponseEntity<AppDto.ApiResponse<User>> getProfile(
            @RequestHeader("Authorization") String authHeader
    ) {
        Long userId = jwtUtil.extractUserId(authHeader.substring(7));
        User user = userService.getProfile(userId);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Profile fetched", user));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<List<User>>> getAllUsers() {
        return ResponseEntity.ok(
            AppDto.ApiResponse.ok("All users", userService.getAllUsers())
        );
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL')")
    public ResponseEntity<AppDto.ApiResponse<AppDto.DashboardStats>> getStats() {
        return ResponseEntity.ok(
            AppDto.ApiResponse.ok("Dashboard stats", userService.getDashboardStats())
        );
    }

    @PostMapping("/admin/create-user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<User>> createUserByAdmin(
            @RequestBody AuthDto.RegisterRequest request
    ) {
        User user = userService.createUserByAdmin(request);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("User account created successfully", user));
    }

    @PutMapping("/admin/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<User>> toggleActive(@PathVariable Long id) {
        User user = userService.toggleUserActive(id);
        String status = user.isActive() ? "activated" : "deactivated";
        return ResponseEntity.ok(AppDto.ApiResponse.ok("User " + status, user));
    }

    @GetMapping("/by-role")
    @PreAuthorize("hasAnyRole('ADMIN','HOD','PRINCIPAL')")
    public ResponseEntity<AppDto.ApiResponse<List<User>>> getByRole(
            @RequestParam User.Role role
    ) {
        return ResponseEntity.ok(
            AppDto.ApiResponse.ok("Users by role", userService.getUsersByRole(role))
        );
    }

    @GetMapping("/by-dept")
    @PreAuthorize("hasAnyRole('ADMIN','HOD','PRINCIPAL','FACULTY')")
    public ResponseEntity<AppDto.ApiResponse<List<User>>> getByDept(
            @RequestParam String dept
    ) {
        return ResponseEntity.ok(
            AppDto.ApiResponse.ok("Students in " + dept, userService.getUsersByDept(dept))
        );
    }

    @GetMapping("/faculty-advisors")
    public ResponseEntity<AppDto.ApiResponse<List<User>>> getFacultyAdvisors(
            @RequestParam(required = false) String dept
    ) {
        return ResponseEntity.ok(
            AppDto.ApiResponse.ok("Faculty advisors", userService.getFacultyAdvisors(dept))
        );
    }

    @GetMapping("/department-members")
    @PreAuthorize("hasAnyRole('ADMIN','PRINCIPAL','HOD','FACULTY')")
    public ResponseEntity<AppDto.ApiResponse<java.util.Map<String, Object>>> getDepartmentMembers(
            @RequestParam String dept
    ) {
        return ResponseEntity.ok(
            AppDto.ApiResponse.ok("Department members for " + dept, userService.getDepartmentMembers(dept))
        );
    }

    @PostMapping("/faculty/students")
    @PreAuthorize("hasAnyRole('FACULTY','HOD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<User>> addStudentForFaculty(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody AuthDto.RegisterRequest request
    ) {
        Long facultyId = jwtUtil.extractUserId(authHeader.substring(7));
        User student = userService.addStudentForFaculty(facultyId, request);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Student added successfully", student));
    }

    @PostMapping("/hod/faculty")
    @PreAuthorize("hasAnyRole('HOD','ADMIN','PRINCIPAL')")
    public ResponseEntity<AppDto.ApiResponse<User>> addFacultyForHod(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody AuthDto.RegisterRequest request
    ) {
        Long hodId = jwtUtil.extractUserId(authHeader.substring(7));
        User faculty = userService.addFacultyForHod(hodId, request);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Faculty member added successfully", faculty));
    }

    @PutMapping("/change-password")
    public ResponseEntity<AppDto.ApiResponse<Void>> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody AuthDto.ChangePasswordRequest request
    ) {
        Long userId = jwtUtil.extractUserId(authHeader.substring(7));
        userService.changePassword(userId, request);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Password changed successfully", null));
    }

    @PutMapping("/profile-picture")
    public ResponseEntity<AppDto.ApiResponse<User>> updateProfilePicture(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody AuthDto.ProfilePictureRequest request
    ) {
        Long userId = jwtUtil.extractUserId(authHeader.substring(7));
        User updated = userService.updateProfilePic(userId, request.getProfilePic());
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Profile picture updated", updated));
    }

    // ─── Advisor Request Endpoints (under /api/users for universal authenticated access) ───

    @PostMapping("/advisor-request/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AppDto.ApiResponse<AdvisorRequest>> submitAdvisorRequest(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody AdvisorDto.AdvisorRequestSubmitDto request
    ) {
        Long studentId = jwtUtil.extractUserId(authHeader.substring(7));
        AdvisorRequest result = advisorRequestService.submitAdvisorRequest(studentId, request);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Advisor request submitted successfully", result));
    }

    @GetMapping("/advisor-request/student-status")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AppDto.ApiResponse<AdvisorDto.StudentAdvisorStatusDto>> getStudentAdvisorStatus(
            @RequestHeader("Authorization") String authHeader
    ) {
        Long studentId = jwtUtil.extractUserId(authHeader.substring(7));
        AdvisorDto.StudentAdvisorStatusDto status = advisorRequestService.getStudentAdvisorStatus(studentId);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Student advisor status retrieved", status));
    }

    @GetMapping("/advisor-request/faculty-list")
    @PreAuthorize("hasAnyRole('FACULTY','HOD','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<List<AdvisorRequest>>> getFacultyAdvisorRequests(
            @RequestHeader("Authorization") String authHeader
    ) {
        Long facultyId = jwtUtil.extractUserId(authHeader.substring(7));
        List<AdvisorRequest> list = advisorRequestService.getFacultyAdvisorRequests(facultyId);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Faculty advisor requests retrieved", list));
    }

    @PutMapping("/advisor-request/{id}/respond")
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

    // ─── Student Parent Info Management (Faculty Advisor Only) ───────────

    @PutMapping("/student/{id}/parent-info")
    @PreAuthorize("hasAnyRole('FACULTY','HOD','PRINCIPAL','ADMIN')")
    public ResponseEntity<AppDto.ApiResponse<User>> updateStudentParentInfo(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody AuthDto.ParentInfoRequest request
    ) {
        Long requestingUserId = jwtUtil.extractUserId(authHeader.substring(7));
        User updated = userService.updateStudentParentInfo(requestingUserId, id, request);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Parent contact details updated successfully", updated));
    }
}
