package com.edusync.service;

import com.edusync.dto.AuthDto;
import com.edusync.exception.ForbiddenOperationException;
import com.edusync.exception.ResourceConflictException;
import com.edusync.model.User;
import com.edusync.repository.UserRepository;
import com.edusync.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authManager;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private AuditService auditService;
    @Mock
    private UserDetails userDetails;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtUtil,
                authManager,
                userDetailsService,
                auditService
        );
    }

    @Test
    void testPublicRegisterDefaultsToStudent() {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setName("Anzal Rahman");
        req.setEmail("anzal.r@student.tkmit.ac.in");
        req.setPassword("SecurePass123");
        req.setDepartment("CSE");
        req.setSemester("S4");

        when(userRepository.existsByEmail("anzal.r@student.tkmit.ac.in")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123")).thenReturn("hashedPass");

        User savedUser = User.builder()
                .id(101L)
                .name("Anzal Rahman")
                .email("anzal.r@student.tkmit.ac.in")
                .role(User.Role.STUDENT)
                .department("CSE")
                .semester("S4")
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userDetailsService.loadUserByUsername("anzal.r@student.tkmit.ac.in")).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails, 101L, "STUDENT")).thenReturn("mock-jwt-token");

        AuthDto.AuthResponse resp = authService.register(req);

        assertNotNull(resp);
        assertEquals("STUDENT", resp.getRole());
        assertEquals("mock-jwt-token", resp.getToken());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(User.Role.STUDENT, captor.getValue().getRole());
    }

    @Test
    void testPublicRegisterRejectsAdminRole() {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setName("Hacker Admin");
        req.setEmail("attacker@hack.com");
        req.setPassword("password");
        req.setRole(User.Role.ADMIN);

        when(userRepository.existsByEmail("attacker@hack.com")).thenReturn(false);

        ForbiddenOperationException ex = assertThrows(
                ForbiddenOperationException.class,
                () -> authService.register(req)
        );

        assertTrue(ex.getMessage().contains("restricted to students"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testPublicRegisterRejectsFacultyRole() {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setName("Unauthorized Faculty");
        req.setEmail("fakefaculty@tkmit.ac.in");
        req.setPassword("password");
        req.setRole(User.Role.FACULTY);

        when(userRepository.existsByEmail("fakefaculty@tkmit.ac.in")).thenReturn(false);

        assertThrows(
                ForbiddenOperationException.class,
                () -> authService.register(req)
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    void testPublicRegisterRejectsDuplicateEmail() {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setEmail("existing@tkmit.ac.in");

        when(userRepository.existsByEmail("existing@tkmit.ac.in")).thenReturn(true);

        assertThrows(
                ResourceConflictException.class,
                () -> authService.register(req)
        );
    }
}
