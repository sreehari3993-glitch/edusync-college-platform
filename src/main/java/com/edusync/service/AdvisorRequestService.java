package com.edusync.service;

import com.edusync.dto.AdvisorDto;
import com.edusync.dto.NotificationDto;
import com.edusync.exception.BadRequestException;
import com.edusync.exception.ForbiddenOperationException;
import com.edusync.exception.ResourceNotFoundException;
import com.edusync.model.AdvisorRequest;
import com.edusync.model.User;
import com.edusync.repository.AdvisorRequestRepository;
import com.edusync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdvisorRequestService {

    private final AdvisorRequestRepository requestRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional
    public AdvisorRequest submitAdvisorRequest(Long studentId, AdvisorDto.AdvisorRequestSubmitDto dto) {
        if (dto.getFacultyId() == null) {
            throw new BadRequestException("Faculty advisor selection is required.");
        }

        User student = userRepo.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student user not found", studentId));

        if (student.getRole() != User.Role.STUDENT) {
            throw new ForbiddenOperationException("Only students can request a faculty advisor.");
        }

        User faculty = userRepo.findById(dto.getFacultyId())
                .orElseThrow(() -> new ResourceNotFoundException("Faculty advisor not found", dto.getFacultyId()));

        if (faculty.getRole() != User.Role.FACULTY && faculty.getRole() != User.Role.HOD) {
            throw new BadRequestException("Selected user is not a faculty member or HOD.");
        }

        // Check if there is already a PENDING request for this faculty or another
        Optional<AdvisorRequest> existingOpt = requestRepo.findFirstByStudentOrderByCreatedAtDesc(student);
        if (existingOpt.isPresent() && existingOpt.get().getStatus() == AdvisorRequest.Status.PENDING) {
            AdvisorRequest existing = existingOpt.get();
            if (existing.getFaculty().getId().equals(faculty.getId())) {
                // Update note and timestamp if same faculty
                existing.setMessage(dto.getMessage());
                existing.setCreatedAt(LocalDateTime.now());
                return requestRepo.save(existing);
            } else {
                // Reject old pending request to allow switching
                existing.setStatus(AdvisorRequest.Status.REJECTED);
                existing.setResponseRemark("Replaced by new request to " + faculty.getName());
                existing.setRespondedAt(LocalDateTime.now());
                requestRepo.save(existing);
            }
        }

        AdvisorRequest req = AdvisorRequest.builder()
                .student(student)
                .faculty(faculty)
                .status(AdvisorRequest.Status.PENDING)
                .message(dto.getMessage())
                .createdAt(LocalDateTime.now())
                .build();

        AdvisorRequest saved = requestRepo.save(req);

        // Notify faculty in real-time
        NotificationDto notif = NotificationDto.builder()
                .id(java.util.UUID.randomUUID().toString())
                .title("New Advisor Request")
                .message(student.getName() + " (" + (student.getRollNumber() != null ? student.getRollNumber() : "Student") +
                        ", " + (student.getDepartment() != null ? student.getDepartment() : "") + " " +
                        (student.getSemester() != null ? student.getSemester() : "") + ") requested you as Faculty Advisor.")
                .type("ADVISOR_REQUEST")
                .targetRole("FACULTY")
                .department(faculty.getDepartment())
                .timestamp(LocalDateTime.now())
                .build();

        notificationService.sendToUser(faculty.getId(), notif);

        auditService.log(
                "ADVISOR_REQUEST_SUBMITTED",
                student.getEmail(),
                student.getName(),
                student.getRole(),
                "AdvisorRequest",
                saved.getId().toString(),
                "Student " + student.getName() + " requested " + faculty.getName() + " as advisor"
        );

        return saved;
    }

    public AdvisorDto.StudentAdvisorStatusDto getStudentAdvisorStatus(Long studentId) {
        User student = userRepo.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found", studentId));

        User advisor = student.getAdvisor();
        Optional<AdvisorRequest> latestOpt = requestRepo.findFirstByStudentOrderByCreatedAtDesc(student);

        return AdvisorDto.StudentAdvisorStatusDto.builder()
                .advisorId(advisor != null ? advisor.getId() : null)
                .advisorName(advisor != null ? advisor.getName() : null)
                .advisorEmail(advisor != null ? advisor.getEmail() : null)
                .advisorDepartment(advisor != null ? advisor.getDepartment() : null)
                .advisorPhone(advisor != null ? advisor.getPhone() : null)
                .latestRequest(latestOpt.orElse(null))
                .build();
    }

    public List<AdvisorRequest> getFacultyAdvisorRequests(Long facultyId) {
        User faculty = userRepo.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found", facultyId));

        return requestRepo.findByFacultyOrderByCreatedAtDesc(faculty);
    }

    @Transactional
    public AdvisorRequest respondToRequest(Long facultyId, Long requestId, AdvisorDto.AdvisorRequestActionDto dto) {
        AdvisorRequest req = requestRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Advisor request not found", requestId));

        if (!req.getFaculty().getId().equals(facultyId)) {
            throw new ForbiddenOperationException("You do not have permission to respond to this request.");
        }

        if (req.getStatus() != AdvisorRequest.Status.PENDING) {
            throw new BadRequestException("This advisor request has already been " + req.getStatus().name().toLowerCase() + ".");
        }

        String action = dto.getAction() != null ? dto.getAction().trim().toUpperCase() : "";
        boolean isAccept = action.startsWith("ACCEPT") || action.equals("APPROVED");

        User student = req.getStudent();
        User faculty = req.getFaculty();

        if (isAccept) {
            req.setStatus(AdvisorRequest.Status.ACCEPTED);
            req.setResponseRemark(dto.getRemark() != null ? dto.getRemark() : "Accepted by advisor");
            req.setRespondedAt(LocalDateTime.now());

            // Assign advisor to student in database
            student.setAdvisor(faculty);
            userRepo.save(student);

            // Send notification to student
            NotificationDto notif = NotificationDto.builder()
                    .id(java.util.UUID.randomUUID().toString())
                    .title("Advisor Request Accepted! 🎉")
                    .message("Prof. " + faculty.getName() + " has accepted your Faculty Advisor request.")
                    .type("ADVISOR_ACCEPTED")
                    .targetRole("STUDENT")
                    .department(student.getDepartment())
                    .timestamp(LocalDateTime.now())
                    .build();
            notificationService.sendToUser(student.getId(), notif);

            auditService.log(
                    "ADVISOR_REQUEST_ACCEPTED",
                    faculty.getEmail(),
                    faculty.getName(),
                    faculty.getRole(),
                    "AdvisorRequest",
                    req.getId().toString(),
                    "Faculty " + faculty.getName() + " accepted advisor request from " + student.getName()
            );
        } else {
            req.setStatus(AdvisorRequest.Status.REJECTED);
            req.setResponseRemark(dto.getRemark() != null ? dto.getRemark() : "Declined by faculty");
            req.setRespondedAt(LocalDateTime.now());

            // If student's current advisor was this faculty, we can leave or reset
            if (student.getAdvisor() != null && student.getAdvisor().getId().equals(faculty.getId())) {
                student.setAdvisor(null);
                userRepo.save(student);
            }

            // Send notification to student
            NotificationDto notif = NotificationDto.builder()
                    .id(java.util.UUID.randomUUID().toString())
                    .title("Advisor Request Update")
                    .message("Prof. " + faculty.getName() + " declined your Faculty Advisor request: " + req.getResponseRemark())
                    .type("ADVISOR_REJECTED")
                    .targetRole("STUDENT")
                    .department(student.getDepartment())
                    .timestamp(LocalDateTime.now())
                    .build();
            notificationService.sendToUser(student.getId(), notif);

            auditService.log(
                    "ADVISOR_REQUEST_REJECTED",
                    faculty.getEmail(),
                    faculty.getName(),
                    faculty.getRole(),
                    "AdvisorRequest",
                    req.getId().toString(),
                    "Faculty " + faculty.getName() + " rejected advisor request from " + student.getName()
            );
        }

        return requestRepo.save(req);
    }
}
