package com.edusync.service;

import com.edusync.dto.AuthDto;
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
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ResourceConflictException("Email already registered: " + req.getEmail());
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
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(User.Role.STUDENT)
                .department(req.getDepartment())
                .semester(req.getSemester())
                .section(req.getSection())
                .rollNumber(req.getRollNumber())
                .employeeId(req.getEmployeeId())
                .phone(req.getPhone())
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
        // Spring Security validates credentials
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", req.getEmail()));

        if (!user.isActive()) {
            throw new ForbiddenOperationException("Account is deactivated. Contact admin.");
        }

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
