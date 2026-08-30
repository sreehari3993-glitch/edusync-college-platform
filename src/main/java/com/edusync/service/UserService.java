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

    public List<User> getFacultyAdvisors(String dept) {
        String cleanDept = clean(dept);
        if (cleanDept != null && !cleanDept.equalsIgnoreCase("ALL")) {
            return userRepo.findByRoleAndDepartment(User.Role.FACULTY, cleanDept);
        }
        return userRepo.findByRole(User.Role.FACULTY);
    }

    public java.util.Map<String, Object> getDepartmentMembers(String dept) {
        String cleanDept = clean(dept);
        if (cleanDept == null) {
            cleanDept = "CSE";
        }
        List<User> faculty = userRepo.findByRoleAndDepartment(User.Role.FACULTY, cleanDept);
        List<User> hods = userRepo.findByRoleAndDepartment(User.Role.HOD, cleanDept);
        List<User> students = userRepo.findByRoleAndDepartment(User.Role.STUDENT, cleanDept);

        List<User> allFaculty = new java.util.ArrayList<>();
        allFaculty.addAll(hods);
        allFaculty.addAll(faculty);

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("department", cleanDept);
        result.put("hod", hods.isEmpty() ? null : hods.get(0));
        result.put("faculty", allFaculty);
        result.put("students", students);
        result.put("totalFaculty", allFaculty.size());
        result.put("totalStudents", students.size());
        return result;
    }

    @Transactional
    public User createUserByAdmin(AuthDto.RegisterRequest req) {
        String cleanEmail = req.getEmail() != null ? req.getEmail().trim().toLowerCase() : "";
        if (cleanEmail.isEmpty() || clean(req.getName()) == null || clean(req.getPassword()) == null) {
            throw new BadRequestException("Name, email, and password are required.");
        }

        if (userRepo.existsByEmailIgnoreCase(cleanEmail)) {
            throw new ResourceConflictException("An account with email '" + cleanEmail + "' is already registered in EduSync.");
        }

        User.Role role = req.getRole() != null ? req.getRole() : User.Role.STUDENT;

        // Admin and Principal roles are institution-wide and do not require a department
        String dept = (role == User.Role.ADMIN || role == User.Role.PRINCIPAL) ? null : clean(req.getDepartment());

        User advisor = null;
        if (role == User.Role.STUDENT && req.getAdvisorId() != null) {
            advisor = userRepo.findById(req.getAdvisorId()).orElse(null);
        }

        User user = User.builder()
                .name(req.getName().trim())
                .email(cleanEmail)
                .password(passwordEncoder.encode(req.getPassword()))
                .role(role)
                .department(dept)
                .semester(clean(req.getSemester()))
                .section(clean(req.getSection()))
                .rollNumber(clean(req.getRollNumber()))
                .employeeId(clean(req.getEmployeeId()))
                .phone(clean(req.getPhone()))
                .advisor(advisor)
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

        String cleanEmail = req.getEmail() != null ? req.getEmail().trim().toLowerCase() : "";
        if (cleanEmail.isEmpty() || clean(req.getName()) == null || clean(req.getPassword()) == null || clean(req.getSemester()) == null) {
            throw new BadRequestException("Name, email, semester and temporary password are required.");
        }

        if (userRepo.existsByEmailIgnoreCase(cleanEmail)) {
            throw new ResourceConflictException("An account with email '" + cleanEmail + "' is already registered in EduSync.");
        }

        User advisor = faculty;
        if (req.getAdvisorId() != null) {
            advisor = userRepo.findById(req.getAdvisorId()).orElse(faculty);
        }

        User student = User.builder()
                .name(req.getName().trim())
                .email(cleanEmail)
                .password(passwordEncoder.encode(req.getPassword()))
                .role(User.Role.STUDENT)
                .department(department)
                .semester(req.getSemester().trim())
                .section(clean(req.getSection()))
                .rollNumber(clean(req.getRollNumber()))
                .phone(clean(req.getPhone()))
                .advisor(advisor)
                .build();
        User saved = userRepo.save(student);

        auditService.log(
                "STUDENT_ADDED",
                faculty.getEmail(),
                faculty.getName(),
                faculty.getRole(),
                "User",
                saved.getId().toString(),
                "Added student " + saved.getName() + " (" + saved.getEmail() + ") to " + department + " (Advisor: " + advisor.getName() + ")"
        );

        return saved;
    }

    @Transactional
    public User addFacultyForHod(Long hodId, AuthDto.RegisterRequest req) {
        User hod = userRepo.findById(hodId)
                .orElseThrow(() -> new ResourceNotFoundException("HOD user not found", hodId));

        if (hod.getRole() != User.Role.HOD && hod.getRole() != User.Role.ADMIN && hod.getRole() != User.Role.PRINCIPAL) {
            throw new ForbiddenOperationException("Only HOD, Admin, or Principal can add faculty members.");
        }

        String department = clean(hod.getDepartment());
        if (department == null && req.getDepartment() != null) {
            department = clean(req.getDepartment());
        }
        if (department == null) {
            throw new BadRequestException("Department is required to register faculty members.");
        }

        String cleanEmail = req.getEmail() != null ? req.getEmail().trim().toLowerCase() : "";
        if (cleanEmail.isEmpty() || clean(req.getName()) == null || clean(req.getPassword()) == null) {
            throw new BadRequestException("Name, email, and temporary password are required.");
        }

        if (userRepo.existsByEmailIgnoreCase(cleanEmail)) {
            throw new ResourceConflictException("An account with email '" + cleanEmail + "' is already registered in EduSync.");
        }

        User faculty = User.builder()
                .name(req.getName().trim())
                .email(cleanEmail)
                .password(passwordEncoder.encode(req.getPassword()))
                .role(User.Role.FACULTY)
                .department(department)
                .employeeId(clean(req.getEmployeeId()))
                .phone(clean(req.getPhone()))
                .build();

        User saved = userRepo.save(faculty);

        auditService.log(
                "FACULTY_ADDED_BY_HOD",
                hod.getEmail(),
                hod.getName(),
                hod.getRole(),
                "User",
                saved.getId().toString(),
                "HOD " + hod.getName() + " added faculty " + saved.getName() + " (" + saved.getEmail() + ") to " + department
        );

        return saved;
    }

    // ─── Update Student Parent Info (Strict Advisor Permission) ────────────

    @Transactional
    public User updateStudentParentInfo(Long requestingUserId, Long studentId, AuthDto.ParentInfoRequest req) {
        User student = userRepo.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student user not found", studentId));

        if (student.getRole() != User.Role.STUDENT) {
            throw new BadRequestException("Target user is not a student.");
        }

        User requestingUser = userRepo.findById(requestingUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", requestingUserId));

        // Validation: Only assigned Faculty Advisor (or Admin / Principal / Dept HOD) can update parent info
        boolean isAdvisor = student.getAdvisor() != null && student.getAdvisor().getId().equals(requestingUserId);
        boolean isAdminOrPrincipal = requestingUser.getRole() == User.Role.ADMIN || requestingUser.getRole() == User.Role.PRINCIPAL;
        boolean isDeptHod = requestingUser.getRole() == User.Role.HOD && 
                requestingUser.getDepartment() != null && 
                requestingUser.getDepartment().equalsIgnoreCase(student.getDepartment());

        if (!isAdvisor && !isAdminOrPrincipal && !isDeptHod) {
            String advisorName = student.getAdvisor() != null ? student.getAdvisor().getName() : "None Assigned";
            String formattedAdvisor = advisorName.startsWith("Prof.") ? advisorName : ("Prof. " + advisorName);
            throw new ForbiddenOperationException(
                "Access Denied: Only the student's assigned Faculty Advisor (" + formattedAdvisor + ") can add or update parent contact information."
            );
        }

        String cleanEmail = clean(req.getParentEmail());
        String cleanPhone = clean(req.getParentPhone());

        student.setParentEmail(cleanEmail != null ? cleanEmail.toLowerCase() : null);
        student.setParentPhone(cleanPhone);
        student.setUpdatedAt(LocalDateTime.now());

        User saved = userRepo.save(student);

        auditService.log(
                "PARENT_INFO_UPDATED",
                requestingUser.getEmail(),
                requestingUser.getName(),
                requestingUser.getRole(),
                "User",
                saved.getId().toString(),
                "Advisor " + requestingUser.getName() + " updated parent contact for student " + saved.getName() + " (" + saved.getRollNumber() + ")"
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
