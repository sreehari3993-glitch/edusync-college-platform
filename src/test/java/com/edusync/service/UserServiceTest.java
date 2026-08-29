package com.edusync.service;

import com.edusync.dto.AuthDto;
import com.edusync.exception.ResourceConflictException;
import com.edusync.model.User;
import com.edusync.repository.LeaveRequestRepository;
import com.edusync.repository.NoticeRepository;
import com.edusync.repository.PlacementDriveRepository;
import com.edusync.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepo;
    @Mock
    private LeaveRequestRepository leaveRepo;
    @Mock
    private NoticeRepository noticeRepo;
    @Mock
    private PlacementDriveRepository driveRepo;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuditService auditService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepo,
                leaveRepo,
                noticeRepo,
                driveRepo,
                passwordEncoder,
                auditService
        );
    }

    @Test
    void testAdminCanCreateFacultyUser() {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setName("Dr. Sreehari");
        req.setEmail("sreehari@tkmit.ac.in");
        req.setPassword("FacultyPass123");
        req.setRole(User.Role.FACULTY);
        req.setDepartment("CSE");
        req.setEmployeeId("TKMIT-FAC-007");

        when(userRepo.existsByEmail("sreehari@tkmit.ac.in")).thenReturn(false);
        when(passwordEncoder.encode("FacultyPass123")).thenReturn("encodedPassword");

        User savedFaculty = User.builder()
                .id(50L)
                .name("Dr. Sreehari")
                .email("sreehari@tkmit.ac.in")
                .role(User.Role.FACULTY)
                .department("CSE")
                .employeeId("TKMIT-FAC-007")
                .build();
        when(userRepo.save(any(User.class))).thenReturn(savedFaculty);

        User result = userService.createUserByAdmin(req);

        assertNotNull(result);
        assertEquals(User.Role.FACULTY, result.getRole());
        assertEquals("sreehari@tkmit.ac.in", result.getEmail());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(captor.capture());
        assertEquals(User.Role.FACULTY, captor.getValue().getRole());
        assertEquals("encodedPassword", captor.getValue().getPassword());
    }

    @Test
    void testAdminCreateUserRejectsDuplicateEmail() {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setName("Admin User");
        req.setEmail("existing@tkmit.ac.in");
        req.setPassword("password");
        req.setRole(User.Role.ADMIN);

        when(userRepo.existsByEmail("existing@tkmit.ac.in")).thenReturn(true);

        assertThrows(
                ResourceConflictException.class,
                () -> userService.createUserByAdmin(req)
        );
    }
}
