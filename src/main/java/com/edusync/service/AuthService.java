package com.edusync.service;

import com.edusync.dto.AuthDto;
import com.edusync.exception.BadRequestException;
import com.edusync.exception.ForbiddenOperationException;
import com.edusync.exception.ResourceConflictException;
import com.edusync.exception.ResourceNotFoundException;
import com.edusync.model.User;
import com.edusync.repository.UserRepository;
import com.edusync.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Authentication service.
 * Handles register + login → returns JWT token.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authManager;
    private final UserDetailsService userDetailsService;
    private final AuditService auditService;

    // ─── Register ─────────────────────────────────────────────────────────

    public AuthDto.AuthResponse register(AuthDto.RegisterRequest req) {
        if (req.getEmail() == null || req.getEmail().trim().isEmpty()) {
            throw new BadRequestException("Email address is required.");
        }
        String cleanEmail = req.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(cleanEmail)) {
            throw new ResourceConflictException("An account with email '" + cleanEmail + "' already exists in EduSync.");
        }

        // Prevent privilege escalation: public self-registration is strictly for students
        if (req.getRole() != null && req.getRole() != User.Role.STUDENT) {
            throw new ForbiddenOperationException(
                    "Public registration is restricted to students. Administrative and faculty accounts must be provisioned by an administrator."
            );
        }

        User advisor = null;
        if (req.getAdvisorId() != null) {
            advisor = userRepository.findById(req.getAdvisorId()).orElse(null);
        }

        User user = User.builder()
                .name(req.getName() != null ? req.getName().trim() : "")
                .email(cleanEmail)
                .password(passwordEncoder.encode(req.getPassword()))
                .role(User.Role.STUDENT)
                .department(req.getDepartment() != null ? req.getDepartment().trim() : null)
                .semester(req.getSemester() != null ? req.getSemester().trim() : null)
                .section(req.getSection() != null ? req.getSection().trim() : null)
                .rollNumber(req.getRollNumber() != null ? req.getRollNumber().trim() : null)
                .employeeId(req.getEmployeeId() != null ? req.getEmployeeId().trim() : null)
                .phone(req.getPhone() != null ? req.getPhone().trim() : null)
                .advisor(advisor)
                .build();

        User saved = userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(saved.getEmail());
        String token = jwtUtil.generateToken(userDetails, saved.getId(), saved.getRole().name());

        auditService.log(
                "USER_REGISTERED",
                saved.getEmail(),
                saved.getName(),
                saved.getRole(),
                "User",
                saved.getId().toString(),
                "User registered with role " + saved.getRole()
        );

        return AuthDto.AuthResponse.builder()
                .token(token)
                .userId(saved.getId())
                .name(saved.getName())
                .email(saved.getEmail())
                .role(saved.getRole().name())
                .department(saved.getDepartment())
                .semester(saved.getSemester())
                .advisorId(saved.getAdvisor() != null ? saved.getAdvisor().getId() : null)
                .advisorName(saved.getAdvisor() != null ? saved.getAdvisor().getName() : null)
                .message("Registration successful! Welcome to EduSync.")
                .build();
    }

    // ─── Login ────────────────────────────────────────────────────────────

    public AuthDto.AuthResponse login(AuthDto.LoginRequest req) {
        String cleanEmail = req.getEmail() != null ? req.getEmail().trim().toLowerCase() : "";
        if (cleanEmail.isEmpty()) {
            throw new BadRequestException("Email address is required.");
        }

        User user = userRepository.findByEmailIgnoreCase(cleanEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User with email '" + cleanEmail + "' was not found."));

        if (!user.isActive()) {
            throw new ForbiddenOperationException("Your account has been deactivated by administrator. Please contact admin@tkmit.ac.in.");
        }

        // Spring Security validates credentials
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(cleanEmail, req.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails, user.getId(), user.getRole().name());

        auditService.log(
                "USER_LOGIN",
                user.getEmail(),
                user.getName(),
                user.getRole(),
                "User",
                user.getId().toString(),
                "Successful login from web app"
        );

        return AuthDto.AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .department(user.getDepartment())
                .semester(user.getSemester())
                .advisorId(user.getAdvisor() != null ? user.getAdvisor().getId() : null)
                .advisorName(user.getAdvisor() != null ? user.getAdvisor().getName() : null)
                .message("Login successful!")
                .build();
    }
}
