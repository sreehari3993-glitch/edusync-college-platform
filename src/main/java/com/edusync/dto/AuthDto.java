package com.edusync.dto;

import com.edusync.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ─── Auth DTOs ─────────────────────────────────────────────────────────────

public class AuthDto {

    // Request: POST /api/auth/login
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @Email
        @NotBlank
        private String email;

        @NotBlank
        private String password;
    }

    // Request: POST /api/auth/register
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegisterRequest {
        @NotBlank
        private String name;

        @Email
        @NotBlank
        private String email;

        @NotBlank
        private String password;

        private User.Role role;
        private String department;
        private String semester;
        private String section;
        private String rollNumber;
        private String employeeId;
        private String phone;
        private Long advisorId;
    }

    // Response: login / register
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuthResponse {
        private String token;
        private Long userId;
        private String name;
        private String email;
        private String role;
        private String department;
        private String semester;
        private Long advisorId;
        private String advisorName;
        private String message;
    }

    // Request: PUT /api/users/change-password
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangePasswordRequest {
        @NotBlank
        private String currentPassword;

        @NotBlank
        private String newPassword;
    }

    // Request: PUT /api/users/profile-picture
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfilePictureRequest {
        @NotBlank
        private String profilePic;
    }
}
