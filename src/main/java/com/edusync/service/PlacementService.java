package com.edusync.service;

import com.edusync.dto.AppDto;
import com.edusync.exception.ResourceConflictException;
import com.edusync.exception.ResourceNotFoundException;
import com.edusync.model.PlacementApplication;
import com.edusync.model.PlacementDrive;
import com.edusync.model.User;
import com.edusync.repository.PlacementApplicationRepository;
import com.edusync.repository.PlacementDriveRepository;
import com.edusync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Placement service.
 * Manages campus recruitment drives and student applications.
 */
@Service
@RequiredArgsConstructor
public class PlacementService {

    private final PlacementDriveRepository driveRepo;
    private final PlacementApplicationRepository appRepo;
    private final UserRepository userRepo;

    // ─── Placement Officer: Create drive ─────────────────────────────────

    @Transactional
    public PlacementDrive createDrive(AppDto.PlacementDriveDto dto) {
        PlacementDrive drive = PlacementDrive.builder()
                .companyName(dto.getCompanyName())
                .logo(dto.getLogo())
                .role(dto.getRole())
                .description(dto.getDescription())
                .packageLpa(dto.getPackageLpa())
                .driveDate(dto.getDriveDate())
                .lastDateToApply(dto.getLastDateToApply())
                .minCgpa(dto.getMinCgpa())
                .eligibleBranches(dto.getEligibleBranches())
                .eligibleBatch(dto.getEligibleBatch())
                .jobType(dto.getJobType())
                .location(dto.getLocation())
                .build();

        return driveRepo.save(drive);
    }

    // ─── List all drives (public) ─────────────────────────────────────────

    public List<PlacementDrive> getAllDrives() {
        return driveRepo.findAllByOrderByDriveDateDesc();
    }

    public List<PlacementDrive> getUpcomingDrives() {
        return driveRepo.findByStatusOrderByDriveDateDesc(PlacementDrive.DriveStatus.UPCOMING);
    }

    // ─── Student: Apply to drive ──────────────────────────────────────────

    @Transactional
    public PlacementApplication applyToDrive(Long studentId, AppDto.PlacementApplicationDto dto) {
        User student = userRepo.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
        PlacementDrive drive = driveRepo.findById(dto.getDriveId())
                .orElseThrow(() -> new ResourceNotFoundException("Placement drive", dto.getDriveId()));

        if (appRepo.existsByStudentAndDrive(student, drive)) {
            throw new ResourceConflictException("Already applied to this drive.");
        }

        PlacementApplication application = PlacementApplication.builder()
                .student(student)
                .drive(drive)
                .cgpa(dto.getCgpa())
                .status(PlacementApplication.ApplicationStatus.APPLIED)
                .aiScore(simulateAiScore())
                .build();

        return appRepo.save(application);
    }

    // ─── Student: View own applications ──────────────────────────────────

    public List<PlacementApplication> getStudentApplications(Long studentId) {
        User student = userRepo.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
        return appRepo.findByStudent(student);
    }

    // ─── Placement Officer: Update application status ────────────────────

    @Transactional
    public PlacementApplication updateStatus(Long appId, PlacementApplication.ApplicationStatus status) {
        PlacementApplication app = appRepo.findById(appId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", appId));
        app.setStatus(status);
        return appRepo.save(app);
    }

    // ─── Placement Officer: All applicants for a drive ────────────────────

    public List<PlacementApplication> getDriveApplicants(Long driveId) {
        PlacementDrive drive = driveRepo.findById(driveId)
                .orElseThrow(() -> new ResourceNotFoundException("Placement drive", driveId));
        return appRepo.findByDrive(drive);
    }

    // ─── Update drive status ──────────────────────────────────────────────

    @Transactional
    public PlacementDrive updateDriveStatus(Long driveId, PlacementDrive.DriveStatus status) {
        PlacementDrive drive = driveRepo.findById(driveId)
                .orElseThrow(() -> new ResourceNotFoundException("Placement drive", driveId));
        drive.setStatus(status);
        return driveRepo.save(drive);
    }

    // Simulates AI resume screening score (0–100)
    private int simulateAiScore() {
        return (int) (Math.random() * 30 + 65); // 65–95 range
    }
}
