package com.edusync.controller;

import com.edusync.dto.AppDto;
import com.edusync.dto.AuthDto;
import com.edusync.model.User;
import com.edusync.security.JwtUtil;
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
}
