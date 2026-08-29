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
    void testAdminCreatePrincipalUserRequiresNoDepartment() {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setName("Dr. Principal");
        req.setEmail("principal@tkmit.ac.in");
        req.setPassword("PrincipalPass123");
        req.setRole(User.Role.PRINCIPAL);
        req.setDepartment("CSE"); // even if department is provided, should be ignored/null for Principal

        when(userRepo.existsByEmail("principal@tkmit.ac.in")).thenReturn(false);
        when(passwordEncoder.encode("PrincipalPass123")).thenReturn("encodedPassword");

        User savedPrincipal = User.builder()
                .id(2L)
                .name("Dr. Principal")
                .email("principal@tkmit.ac.in")
                .role(User.Role.PRINCIPAL)
                .department(null)
                .build();
        when(userRepo.save(any(User.class))).thenReturn(savedPrincipal);

        User result = userService.createUserByAdmin(req);

        assertNotNull(result);
        assertEquals(User.Role.PRINCIPAL, result.getRole());
        assertNull(result.getDepartment());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(captor.capture());
        assertNull(captor.getValue().getDepartment());
    }

    @Test
    void testFacultyAddStudentWithCustomAdvisor() {
        AuthDto.RegisterRequest req = new AuthDto.RegisterRequest();
        req.setName("Kavya Nair");
        req.setEmail("kavya.n@student.tkmit.ac.in");
        req.setPassword("tempPass");
        req.setSemester("S4");
        req.setAdvisorId(25L);

        User faculty = User.builder()
                .id(20L)
                .name("Prof. Alex")
                .department("CSE")
                .role(User.Role.FACULTY)
                .build();

        User selectedAdvisor = User.builder()
                .id(25L)
                .name("Prof. Sarah")
                .department("CSE")
                .role(User.Role.FACULTY)
                .build();

        when(userRepo.findById(20L)).thenReturn(java.util.Optional.of(faculty));
        when(userRepo.findById(25L)).thenReturn(java.util.Optional.of(selectedAdvisor));
        when(userRepo.existsByEmail("kavya.n@student.tkmit.ac.in")).thenReturn(false);
        when(passwordEncoder.encode("tempPass")).thenReturn("hashedTemp");

        User savedStudent = User.builder()
                .id(105L)
                .name("Kavya Nair")
                .email("kavya.n@student.tkmit.ac.in")
                .role(User.Role.STUDENT)
                .department("CSE")
                .semester("S4")
                .advisor(selectedAdvisor)
                .build();
        when(userRepo.save(any(User.class))).thenReturn(savedStudent);

        User result = userService.addStudentForFaculty(20L, req);

        assertNotNull(result);
        assertEquals("Kavya Nair", result.getName());
        assertEquals("CSE", result.getDepartment());
        assertNotNull(result.getAdvisor());
        assertEquals("Prof. Sarah", result.getAdvisor().getName());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(captor.capture());
        assertEquals("Prof. Sarah", captor.getValue().getAdvisor().getName());
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
