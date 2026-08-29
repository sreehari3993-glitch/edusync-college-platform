package com.edusync.service;

import com.edusync.dto.AppDto;
import com.edusync.exception.BadRequestException;
import com.edusync.exception.ForbiddenOperationException;
import com.edusync.exception.ResourceNotFoundException;
import com.edusync.model.Assignment;
import com.edusync.model.User;
import com.edusync.repository.AssignmentRepository;
import com.edusync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Transactional
    public AppDto.AssignmentResponse createAssignment(Long facultyId, AppDto.AssignmentDto dto) {
        User faculty = userRepo.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException("User", facultyId));
        String department = clean(faculty.getDepartment());
        if (department == null) {
            throw new ForbiddenOperationException("Your account has no department set. Assignment recipients need a department.");
        }

        String subject = clean(dto.getSubject());
        String title = clean(dto.getTitle());
        String description = clean(dto.getDescription());
        if (subject == null || title == null || description == null || dto.getDueDate() == null) {
            throw new BadRequestException("Subject, title, description and due date are required.");
        }

        Integer maxMarks = dto.getMaxMarks() != null ? dto.getMaxMarks() : 20;
        if (maxMarks <= 0) {
            throw new BadRequestException("Max marks must be greater than zero.");
        }

        Assignment assignment = Assignment.builder()
                .subject(subject)
                .title(title)
                .description(description)
                .department(department)
                .semester(cleanSemester(dto.getSemester()))
                .maxMarks(maxMarks)
                .dueDate(dto.getDueDate())
                .createdBy(faculty)
                .build();

        Assignment saved = assignmentRepo.save(assignment);

        // Notify students in department via real-time SSE
        notificationService.sendToRoleAndDepartment(
                User.Role.STUDENT,
                department,
                com.edusync.dto.NotificationDto.builder()
                        .type("ASSIGNMENT_NEW")
                        .title("📚 New Assignment: " + subject)
                        .message(title + " (Due: " + saved.getDueDate() + ")")
                        .actorName(faculty.getName())
                        .department(department)
                        .link("#page-assignments")
                        .build()
        );

        // Dispatch Email notifications to students in department
        List<User> students = userRepo.findByRoleAndDepartment(User.Role.STUDENT, department);
        for (User student : students) {
            if (student.getEmail() != null && !student.getEmail().isBlank()) {
                emailService.sendAssignmentNotificationEmail(
                        student.getEmail(),
                        student.getName(),
                        saved.getSubject(),
                        saved.getTitle(),
                        saved.getDescription(),
                        saved.getDueDate().toString(),
                        saved.getMaxMarks(),
                        faculty.getName()
                );
            }
        }

        return AppDto.AssignmentResponse.from(saved, countRecipients(saved));
    }

    @Transactional(readOnly = true)
    public List<AppDto.AssignmentResponse> getFacultyAssignments(Long facultyId) {
        User faculty = userRepo.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException("User", facultyId));
        String department = clean(faculty.getDepartment());
        if (department == null) {
            return List.of();
        }
        return assignmentRepo.findByActiveTrueAndDepartmentOrderByCreatedAtDesc(department)
                .stream()
                .map(a -> AppDto.AssignmentResponse.from(a, countRecipients(a)))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppDto.AssignmentResponse> getStudentAssignments(Long studentId) {
        User student = userRepo.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", studentId));
        String department = clean(student.getDepartment());
        if (department == null) {
            return List.of();
        }
        String semester = clean(student.getSemester());
        return assignmentRepo.findVisibleToStudent(department, semester == null ? "" : semester)
                .stream()
                .map(a -> AppDto.AssignmentResponse.from(a, countRecipients(a)))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAssignment(Long userId, Long assignmentId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Assignment assignment = assignmentRepo.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", assignmentId));
        boolean sameDepartment = clean(user.getDepartment()) != null
                && clean(user.getDepartment()).equals(assignment.getDepartment());
        if (user.getRole() != User.Role.ADMIN && !sameDepartment) {
            throw new ForbiddenOperationException("You can only remove assignments from your department.");
        }
        assignment.setActive(false);
        assignmentRepo.save(assignment);
    }

    private int countRecipients(Assignment assignment) {
        return (int) userRepo.findByDepartmentAndRole(assignment.getDepartment(), User.Role.STUDENT)
                .stream()
                .filter(User::isActive)
                .filter(s -> assignment.getSemester() == null || assignment.getSemester().equals(clean(s.getSemester())))
                .count();
    }

    private String cleanSemester(String value) {
        String cleaned = clean(value);
        if (cleaned == null || "ALL".equalsIgnoreCase(cleaned)) {
            return null;
        }
        return cleaned;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
