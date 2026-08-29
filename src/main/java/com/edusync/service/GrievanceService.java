package com.edusync.service;

import com.edusync.dto.AppDto;
import com.edusync.exception.ResourceNotFoundException;
import com.edusync.model.Grievance;
import com.edusync.model.User;
import com.edusync.repository.GrievanceRepository;
import com.edusync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GrievanceService {

    private final GrievanceRepository grievanceRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public Grievance submitGrievance(Long studentId, AppDto.GrievanceDto dto) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        Grievance grievance = Grievance.builder()
                .student(student)
                .category(dto.getCategory())
                .subject(dto.getSubject())
                .description(dto.getDescription())
                .targetRole(dto.getTargetRole())
                .department(student.getDepartment())
                .anonymous(dto.isAnonymous())
                .status(Grievance.Status.OPEN)
                .build();

        Grievance saved = grievanceRepository.save(grievance);

        auditService.log(
                "GRIEVANCE_SUBMITTED",
                dto.isAnonymous() ? "anonymous@student" : student.getEmail(),
                dto.isAnonymous() ? "Anonymous Student" : student.getName(),
                student.getRole(),
                "Grievance",
                saved.getId().toString(),
                "Submitted " + dto.getCategory() + " grievance to " + dto.getTargetRole()
        );

        return saved;
    }

    public List<Grievance> getMyGrievances(Long studentId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
        return grievanceRepository.findByStudentOrderByCreatedAtDesc(student);
    }

    public List<Grievance> getGrievancesForRole(User user) {
        if (user.getRole() == User.Role.HOD) {
            return grievanceRepository.findByTargetRoleAndDepartmentOrderByCreatedAtDesc(
                    Grievance.TargetRole.HOD, user.getDepartment()
            );
        } else if (user.getRole() == User.Role.PRINCIPAL || user.getRole() == User.Role.ADMIN) {
            return grievanceRepository.findByTargetRoleOrderByCreatedAtDesc(Grievance.TargetRole.PRINCIPAL);
        }
        return List.of();
    }

    @Transactional
    public Grievance respondToGrievance(Long userId, Long grievanceId, AppDto.GrievanceReplyDto dto) {
        User responder = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Grievance grievance = grievanceRepository.findById(grievanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Grievance", grievanceId));

        grievance.setStatus(dto.getStatus() != null ? dto.getStatus() : Grievance.Status.RESOLVED);
        grievance.setResponse(dto.getResponse());
        grievance.setRespondedBy(responder.getName() + " (" + responder.getRole() + ")");
        grievance.setRespondedAt(LocalDateTime.now());

        Grievance updated = grievanceRepository.save(grievance);

        auditService.log(
                "GRIEVANCE_RESOLVED",
                responder.getEmail(),
                responder.getName(),
                responder.getRole(),
                "Grievance",
                updated.getId().toString(),
                "Resolved grievance #" + updated.getId() + " with status " + updated.getStatus()
        );

        return updated;
    }
}
