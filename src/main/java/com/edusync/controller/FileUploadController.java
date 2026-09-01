package com.edusync.controller;

import com.edusync.dto.AppDto.ApiResponse;
import com.edusync.dto.DriveFileDto;
import com.edusync.model.Assignment;
import com.edusync.model.AssignmentSubmission;
import com.edusync.model.StudyMaterial;
import com.edusync.model.User;
import com.edusync.repository.AssignmentRepository;
import com.edusync.repository.AssignmentSubmissionRepository;
import com.edusync.repository.StudyMaterialRepository;
import com.edusync.repository.UserRepository;
import com.edusync.service.GoogleDriveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin
public class FileUploadController {

    private final GoogleDriveService googleDriveService;
    private final UserRepository userRepository;
    private final StudyMaterialRepository studyMaterialRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;

    /**
     * Universal file upload endpoint to Google Drive.
     * Subfolders: "Assignments", "Study-Materials", "Duty-Leave-Certificates", "Profile-Pictures", "ATS-Resumes"
     */
    @PostMapping("/files/upload")
    public ResponseEntity<ApiResponse<DriveFileDto>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "GENERAL") String category,
            @AuthenticationPrincipal User user) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("File is required and cannot be empty."));
        }

        try {
            String subfolder = mapCategoryToSubfolder(category);
            String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
            String prefix = user != null ? user.getId() + "_" : "";
            String gDriveFileName = prefix + System.currentTimeMillis() + "_" + originalFilename;

            byte[] bytes = file.getBytes();
            String mimeType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

            DriveFileDto result = googleDriveService.uploadFile(subfolder, gDriveFileName, mimeType, bytes);
            result.setCategory(category);
            result.setOriginalFileName(originalFilename);
            if (user != null) {
                result.setUploadedBy(user.getName());
            }

            log.info("☁️ [Cloud Storage] Successfully uploaded {} ({} bytes) into Google Drive subfolder: {}",
                    originalFilename, bytes.length, subfolder);

            return ResponseEntity.ok(ApiResponse.ok("File uploaded successfully to Google Drive", result));
        } catch (Exception e) {
            log.error("❌ [Cloud Storage] Upload failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to upload file to Google Drive: " + e.getMessage()));
        }
    }

    /**
     * Direct file download stream from Google Drive.
     */
    @GetMapping("/files/download/{fileId}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String fileId) {
        try {
            byte[] fileBytes = googleDriveService.downloadFile(fileId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"edusync_file_" + fileId + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(fileBytes);
        } catch (Exception e) {
            log.error("❌ Failed to download file [{}] from Google Drive: {}", fileId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Faculty uploads study material with Google Drive storage.
     */
    @PostMapping("/materials/upload")
    @PreAuthorize("hasAnyRole('FACULTY', 'HOD', 'PRINCIPAL', 'ADMIN')")
    public ResponseEntity<ApiResponse<StudyMaterial>> uploadStudyMaterial(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("subject") String subject,
            @RequestParam(value = "module", required = false) String module,
            @RequestParam(value = "semester", required = false) String semester,
            @AuthenticationPrincipal User user) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("File is required"));
        }

        try {
            String dept = user.getDepartment() != null ? user.getDepartment() : "General";
            String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "material";
            String gDriveFileName = "MAT_" + dept + "_" + (semester != null ? semester + "_" : "") + System.currentTimeMillis() + "_" + originalFilename;

            byte[] bytes = file.getBytes();
            String mimeType = file.getContentType() != null ? file.getContentType() : "application/pdf";

            DriveFileDto uploadResult = googleDriveService.uploadFile("Study-Materials", gDriveFileName, mimeType, bytes);

            String fileExt = "PDF";
            if (originalFilename.contains(".")) {
                fileExt = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toUpperCase();
            }

            StudyMaterial material = StudyMaterial.builder()
                    .title(title)
                    .subject(subject)
                    .module(module)
                    .department(dept)
                    .semester(semester)
                    .fileName(originalFilename)
                    .fileType(fileExt)
                    .fileSize(file.getSize())
                    .fileUrl(uploadResult.getWebViewLink())
                    .downloadUrl(uploadResult.getDownloadUrl())
                    .driveFileId(uploadResult.getFileId())
                    .uploadedBy(user)
                    .build();

            StudyMaterial saved = studyMaterialRepository.save(material);
            return ResponseEntity.ok(ApiResponse.ok("Study material uploaded to Google Drive successfully", saved));
        } catch (Exception e) {
            log.error("Failed to upload study material: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(ApiResponse.error("Study material upload failed: " + e.getMessage()));
        }
    }

    /**
     * Lists study materials for student or faculty.
     */
    @GetMapping("/materials")
    public ResponseEntity<ApiResponse<List<StudyMaterial>>> getStudyMaterials(
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "semester", required = false) String semester,
            @AuthenticationPrincipal User user) {

        String targetDept = department != null ? department : (user != null ? user.getDepartment() : null);
        String targetSem = semester != null ? semester : (user != null ? user.getSemester() : null);

        List<StudyMaterial> materials;
        if (targetDept != null && targetSem != null) {
            materials = studyMaterialRepository.findByDepartmentAndSemesterOrderByCreatedAtDesc(targetDept, targetSem);
        } else if (targetDept != null) {
            materials = studyMaterialRepository.findByDepartmentOrderByCreatedAtDesc(targetDept);
        } else {
            materials = studyMaterialRepository.findAllByOrderByCreatedAtDesc();
        }

        return ResponseEntity.ok(ApiResponse.ok(materials));
    }

    /**
     * Student submits an assignment with file attachment on Google Drive.
     */
    @PostMapping("/assignments/{assignmentId}/submit")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<AssignmentSubmission>> submitAssignment(
            @PathVariable Long assignmentId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User student) {

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElse(null);
        if (assignment == null) {
            return ResponseEntity.notFound().build();
        }

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Assignment submission requires a file (PDF/Doc/Zip)."));
        }

        try {
            String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "submission.pdf";
            String gDriveFileName = "SUB_" + assignmentId + "_" + student.getRollNumber() + "_" + System.currentTimeMillis() + "_" + originalFilename;

            byte[] bytes = file.getBytes();
            String mimeType = file.getContentType() != null ? file.getContentType() : "application/pdf";

            DriveFileDto uploadResult = googleDriveService.uploadFile("Assignments", gDriveFileName, mimeType, bytes);

            AssignmentSubmission submission = assignmentSubmissionRepository
                    .findByAssignmentAndStudent(assignment, student)
                    .orElse(AssignmentSubmission.builder()
                            .assignment(assignment)
                            .student(student)
                            .build());

            submission.setFileName(originalFilename);
            submission.setFileUrl(uploadResult.getWebViewLink());
            submission.setDownloadUrl(uploadResult.getDownloadUrl());
            submission.setDriveFileId(uploadResult.getFileId());
            submission.setFileSize(file.getSize());
            submission.setSubmittedAt(java.time.LocalDateTime.now());
            submission.setStatus(AssignmentSubmission.SubmissionStatus.SUBMITTED);

            AssignmentSubmission saved = assignmentSubmissionRepository.save(submission);
            return ResponseEntity.ok(ApiResponse.ok("Assignment successfully submitted to Google Drive!", saved));
        } catch (Exception e) {
            log.error("Failed to submit assignment: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(ApiResponse.error("Assignment upload failed: " + e.getMessage()));
        }
    }

    /**
     * Updates user profile picture and uploads to Google Drive.
     */
    @PostMapping("/users/profile/photo")
    public ResponseEntity<ApiResponse<String>> uploadProfilePhoto(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Image file is required"));
        }

        try {
            String ext = "jpg";
            if (file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")) {
                ext = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.') + 1);
            }
            String gDriveFileName = "AVATAR_USER_" + user.getId() + "_" + System.currentTimeMillis() + "." + ext;

            DriveFileDto uploadResult = googleDriveService.uploadFile("Profile-Pictures", gDriveFileName, file.getContentType(), file.getBytes());

            user.setProfilePic(uploadResult.getWebViewLink());
            userRepository.save(user);

            return ResponseEntity.ok(ApiResponse.ok("Profile picture updated", uploadResult.getWebViewLink()));
        } catch (Exception e) {
            log.error("Failed to upload profile photo: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(ApiResponse.error("Photo upload failed: " + e.getMessage()));
        }
    }

    private String mapCategoryToSubfolder(String category) {
        if (category == null) return "General";
        return switch (category.trim().toUpperCase()) {
            case "ASSIGNMENT", "ASSIGNMENTS" -> "Assignments";
            case "STUDY_MATERIAL", "STUDY_MATERIALS", "MATERIALS" -> "Study-Materials";
            case "DUTY_LEAVE", "LEAVE", "OD_CERTIFICATE" -> "Duty-Leave-Certificates";
            case "PROFILE", "AVATAR" -> "Profile-Pictures";
            case "RESUME", "ATS" -> "ATS-Resumes";
            default -> "General";
        };
    }
}
