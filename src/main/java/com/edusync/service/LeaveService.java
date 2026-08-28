package com.edusync.service;

import com.edusync.dto.AppDto;
import com.edusync.model.LeaveRequest;
import com.edusync.model.User;
import com.edusync.repository.LeaveRequestRepository;
import com.edusync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Leave request service.
 * Full approval chain: Student → Faculty → HOD → Principal (for long leaves).
 * Leaves > 3 days automatically go to Principal level.
 */
@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRepo;
    private final UserRepository userRepo;
    private final AuditService auditService;
    private final PdfService pdfService;

    // ─── Student: Submit leave ────────────────────────────────────────────

    @Transactional
    public LeaveRequest submitLeave(Long studentId, AppDto.LeaveRequestDto dto) {
        User student = userRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));

        int days = (int) ChronoUnit.DAYS.between(dto.getFromDate(), dto.getToDate()) + 1;

        LeaveRequest leave = LeaveRequest.builder()
                .student(student)
                .leaveType(dto.getLeaveType())
                .reason(dto.getReason())
                .fromDate(dto.getFromDate())
                .toDate(dto.getToDate())
                .noOfDays(days)
                .eventName(dto.getEventName())
                .overallStatus(LeaveRequest.Status.PENDING)
                .facultyStatus(LeaveRequest.Status.PENDING)
                .hodStatus(LeaveRequest.Status.PENDING)
                .principalStatus(
                    days > 3 ? LeaveRequest.Status.PENDING : LeaveRequest.Status.NOT_REQUIRED
                )
                .currentStage("FACULTY_REVIEW")
                .build();

        LeaveRequest saved = leaveRepo.save(leave);

        auditService.log(
                "LEAVE_SUBMITTED",
                student.getEmail(),
                student.getName(),
                student.getRole(),
                "LeaveRequest",
                saved.getId().toString(),
                "Submitted " + saved.getLeaveType() + " (" + saved.getNoOfDays() + " days)"
        );

        return saved;
    }

    // ─── Student: My leaves ───────────────────────────────────────────────

    public List<LeaveRequest> getStudentLeaves(Long studentId) {
        User student = userRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return leaveRepo.findByStudentOrderByCreatedAtDesc(student);
    }

    // ─── Faculty: View pending requests ──────────────────────────────────

    public List<LeaveRequest> getPendingForFaculty(String department) {
        return leaveRepo.findPendingForFaculty(department);
    }

    public List<LeaveRequest> getProcessedForFaculty(String department) {
        return leaveRepo.findProcessedForFaculty(department);
    }

    // ─── Faculty: Approve/Reject ─────────────────────────────────────────

    @Transactional
    public LeaveRequest facultyAction(Long leaveId, Long facultyId, AppDto.LeaveApprovalDto dto) {
        LeaveRequest leave = getLeave(leaveId);
        User faculty = userRepo.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        leave.setFacultyStatus(dto.getStatus());
        leave.setFacultyRemark(dto.getRemark());
        leave.setFacultyApprovedBy(faculty.getName());
        leave.setFacultyApprovedAt(LocalDateTime.now());

        if (dto.getStatus() == LeaveRequest.Status.REJECTED) {
            leave.setOverallStatus(LeaveRequest.Status.REJECTED);
            leave.setCurrentStage("REJECTED_BY_FACULTY");
        } else {
            leave.setCurrentStage("HOD_REVIEW");
        }

        LeaveRequest saved = leaveRepo.save(leave);

        auditService.log(
                dto.getStatus() == LeaveRequest.Status.APPROVED ? "LEAVE_APPROVED_FACULTY" : "LEAVE_REJECTED_FACULTY",
                faculty.getEmail(),
                faculty.getName(),
                faculty.getRole(),
                "LeaveRequest",
                saved.getId().toString(),
                "Faculty decision: " + dto.getStatus() + " for request #" + saved.getId()
        );

        return saved;
    }

    // ─── HOD: View pending requests ───────────────────────────────────────

    public List<LeaveRequest> getPendingForHod(String department) {
        return leaveRepo.findPendingForHod(department);
    }

    // ─── HOD: Approve/Reject ─────────────────────────────────────────────

    @Transactional
    public LeaveRequest hodAction(Long leaveId, Long hodId, AppDto.LeaveApprovalDto dto) {
        LeaveRequest leave = getLeave(leaveId);
        User hod = userRepo.findById(hodId)
                .orElseThrow(() -> new RuntimeException("HOD not found"));

        leave.setHodStatus(dto.getStatus());
        leave.setHodRemark(dto.getRemark());
        leave.setHodApprovedBy(hod.getName());
        leave.setHodApprovedAt(LocalDateTime.now());

        if (dto.getStatus() == LeaveRequest.Status.REJECTED) {
            leave.setOverallStatus(LeaveRequest.Status.REJECTED);
            leave.setCurrentStage("REJECTED_BY_HOD");
        } else if (leave.getPrincipalStatus() == LeaveRequest.Status.NOT_REQUIRED) {
            // HOD is final — mark complete
            leave.setOverallStatus(LeaveRequest.Status.APPROVED);
            leave.setCurrentStage("COMPLETED");
            leave.setPdfPath("/api/leaves/" + leave.getId() + "/pdf");
        } else {
            leave.setCurrentStage("PRINCIPAL_REVIEW");
        }

        LeaveRequest saved = leaveRepo.save(leave);

        auditService.log(
                dto.getStatus() == LeaveRequest.Status.APPROVED ? "LEAVE_APPROVED_HOD" : "LEAVE_REJECTED_HOD",
                hod.getEmail(),
                hod.getName(),
                hod.getRole(),
                "LeaveRequest",
                saved.getId().toString(),
                "HOD decision: " + dto.getStatus() + " for request #" + saved.getId()
        );

        return saved;
    }

    // ─── Principal: View pending requests ────────────────────────────────

    public List<LeaveRequest> getPendingForPrincipal() {
        return leaveRepo.findPendingForPrincipal();
    }

    // ─── Principal: Approve/Reject ────────────────────────────────────────

    @Transactional
    public LeaveRequest principalAction(Long leaveId, AppDto.LeaveApprovalDto dto) {
        LeaveRequest leave = getLeave(leaveId);

        leave.setPrincipalStatus(dto.getStatus());
        leave.setPrincipalRemark(dto.getRemark());
        leave.setPrincipalApprovedAt(LocalDateTime.now());

        if (dto.getStatus() == LeaveRequest.Status.APPROVED) {
            leave.setOverallStatus(LeaveRequest.Status.APPROVED);
            leave.setCurrentStage("COMPLETED");
            leave.setPdfPath("/api/leaves/" + leave.getId() + "/pdf");
        } else {
            leave.setOverallStatus(LeaveRequest.Status.REJECTED);
            leave.setCurrentStage("REJECTED_BY_PRINCIPAL");
        }

        LeaveRequest saved = leaveRepo.save(leave);

        auditService.log(
                dto.getStatus() == LeaveRequest.Status.APPROVED ? "LEAVE_APPROVED_PRINCIPAL" : "LEAVE_REJECTED_PRINCIPAL",
                "principal@tkmit.ac.in",
                "Principal Office",
                User.Role.PRINCIPAL,
                "LeaveRequest",
                saved.getId().toString(),
                "Principal decision: " + dto.getStatus() + " for request #" + saved.getId()
        );

        return saved;
    }

    // ─── PDF Generation ──────────────────────────────────────────────────

    public byte[] generateLeavePdf(Long leaveId) {
        LeaveRequest leave = getLeave(leaveId);
        return pdfService.generateLeaveCertificatePdf(leave);
    }

    // ─── Admin: All requests ──────────────────────────────────────────────

    public List<LeaveRequest> getAllLeaves() {
        return leaveRepo.findAllByOrderByCreatedAtDesc();
    }

    // ─── Department view ─────────────────────────────────────────────────

    public List<LeaveRequest> getLeavesByDept(String dept) {
        return leaveRepo.findByDepartment(dept);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    public LeaveRequest getLeaveById(Long id) {
        return getLeave(id);
    }

    private LeaveRequest getLeave(Long id) {
        return leaveRepo.findWithStudentById(id)
                .orElseThrow(() -> new RuntimeException("Leave request not found: " + id));
    }
}
