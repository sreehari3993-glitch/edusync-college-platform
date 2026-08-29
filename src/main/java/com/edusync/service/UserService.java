package com.edusync.service;

import com.edusync.dto.AppDto;
import com.edusync.dto.AuthDto;
import com.edusync.exception.BadRequestException;
import com.edusync.exception.ForbiddenOperationException;
import com.edusync.exception.ResourceConflictException;
import com.edusync.exception.ResourceNotFoundException;
import com.edusync.model.User;
import com.edusync.repository.LeaveRequestRepository;
import com.edusync.repository.NoticeRepository;
import com.edusync.repository.PlacementDriveRepository;
import com.edusync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * User management service.
 * Handles profile retrieval, password changes, admin user management, dashboard stats.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;
    private final LeaveRequestRepository leaveRepo;
    private final NoticeRepository noticeRepo;
    private final PlacementDriveRepository driveRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    // ─── Get user profile ─────────────────────────────────────────────────

    public User getProfile(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    public User getByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
    }

    // ─── Admin: List all users ────────────────────────────────────────────

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    public List<User> getUsersByRole(User.Role role) {
        return userRepo.findByRole(role);
    }

    public List<User> getUsersByDept(String dept) {
        return userRepo.findByRoleAndDepartment(User.Role.STUDENT, dept);
    }

    @Transactional
    public User createUserByAdmin(AuthDto.RegisterRequest req) {
        if (clean(req.getEmail()) == null || clean(req.getName()) == null || clean(req.getPassword()) == null) {
            throw new BadRequestException("Name, email, and password are required.");
        }

        if (userRepo.existsByEmail(req.getEmail().trim())) {
            throw new ResourceConflictException("Email already registered: " + req.getEmail().trim());
        }

        User.Role role = req.getRole() != null ? req.getRole() : User.Role.STUDENT;

        User user = User.builder()
                .name(req.getName().trim())
                .email(req.getEmail().trim())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(role)
                .department(clean(req.getDepartment()))
                .semester(clean(req.getSemester()))
                .section(clean(req.getSection()))
                .rollNumber(clean(req.getRollNumber()))
                .employeeId(clean(req.getEmployeeId()))
                .phone(clean(req.getPhone()))
                .build();

        User saved = userRepo.save(user);

        auditService.log(
                "ADMIN_USER_PROVISIONED",
                "admin@tkmit.ac.in",
                "Administrator",
                User.Role.ADMIN,
                "User",
                saved.getId().toString(),
                "Admin provisioned " + role + " account for " + saved.getEmail()
        );

        return saved;
    }

    @Transactional
    public User addStudentForFaculty(Long facultyId, AuthDto.RegisterRequest req) {
        User faculty = userRepo.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty user not found"));
        String department = clean(faculty.getDepartment());
        if (department == null) {
            throw new ForbiddenOperationException("Your faculty account needs a department before adding students.");
        }
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new ResourceConflictException("Email already registered: " + req.getEmail());
        }
        if (clean(req.getName()) == null || clean(req.getEmail()) == null || clean(req.getPassword()) == null || clean(req.getSemester()) == null) {
            throw new BadRequestException("Name, email, semester and temporary password are required.");
        }

        User student = User.builder()
                .name(req.getName().trim())
                .email(req.getEmail().trim())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(User.Role.STUDENT)
                .department(department)
                .semester(req.getSemester().trim())
                .section(clean(req.getSection()))
                .rollNumber(clean(req.getRollNumber()))
                .phone(clean(req.getPhone()))
                .build();
        User saved = userRepo.save(student);

        auditService.log(
                "STUDENT_ADDED",
                faculty.getEmail(),
                faculty.getName(),
                faculty.getRole(),
                "User",
                saved.getId().toString(),
                "Added student " + saved.getName() + " (" + saved.getEmail() + ") to " + department
        );

        return saved;
    }

    // ─── Change Password ──────────────────────────────────────────────────

    @Transactional
    public void changePassword(Long userId, AuthDto.ChangePasswordRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password does not match.");
        }

        if (req.getNewPassword() == null || req.getNewPassword().trim().length() < 6) {
            throw new BadRequestException("New password must be at least 6 characters.");
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword().trim()));
        userRepo.save(user);

        auditService.log(
                "PASSWORD_CHANGED",
                user.getEmail(),
                user.getName(),
                user.getRole(),
                "User",
                user.getId().toString(),
                "Password updated successfully"
        );
    }

    // ─── Update Profile Picture ──────────────────────────────────────────

    @Transactional
    public User updateProfilePic(Long userId, String profilePic) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setProfilePic(profilePic);
        return userRepo.save(user);
    }

    // ─── Admin: Toggle user active/inactive ──────────────────────────────

    @Transactional
    public User toggleUserActive(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setActive(!user.isActive());
        User saved = userRepo.save(user);

        auditService.log(
                user.isActive() ? "USER_ACTIVATED" : "USER_DEACTIVATED",
                "admin@tkmit.ac.in",
                "Administrator",
                User.Role.ADMIN,
                "User",
                saved.getId().toString(),
                (user.isActive() ? "Activated" : "Deactivated") + " user " + saved.getEmail()
        );

        return saved;
    }

    // ─── Admin: Dashboard statistics ─────────────────────────────────────

    public AppDto.DashboardStats getDashboardStats() {
        long students = userRepo.findByRole(User.Role.STUDENT).size();
        long faculty  = userRepo.findByRole(User.Role.FACULTY).size();
        long pending  = leaveRepo.countByOverallStatus(
            com.edusync.model.LeaveRequest.Status.PENDING
        );
        long notices = noticeRepo.findActiveNotices(LocalDateTime.now()).size();
        long drives  = driveRepo.findByStatusOrderByDriveDateDesc(
            com.edusync.model.PlacementDrive.DriveStatus.UPCOMING
        ).size();

        return AppDto.DashboardStats.builder()
                .totalStudents(students)
                .totalFaculty(faculty)
                .pendingLeaves(pending)
                .activeNotices(notices)
                .upcomingDrives(drives)
                .totalDepartments(6)   // CSE, ECE, MECH, FT, EEE, CE
                .build();
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // ─── Update profile ───────────────────────────────────────────────────

    @Transactional
    public User updateProfile(Long userId, String phone, String profilePic) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (phone != null)      user.setPhone(phone);
        if (profilePic != null) user.setProfilePic(profilePic);
        return userRepo.save(user);
    }
}
