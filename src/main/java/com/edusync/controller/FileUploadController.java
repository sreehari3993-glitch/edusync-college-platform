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

        try {
            boolean isCert = "DUTY_LEAVE".equalsIgnoreCase(category)
                    || "LEAVE".equalsIgnoreCase(category)
                    || "OD_CERTIFICATE".equalsIgnoreCase(category);
            com.edusync.util.FileUploadValidator.ValidatedFile validated = com.edusync.util.FileUploadValidator.validateUpload(file, isCert);

            String subfolder = mapCategoryToSubfolder(category);
            String originalFilename = validated.getSanitizedFilename();
            String prefix = user != null ? user.getId() + "_" : "";
            String gDriveFileName = prefix + System.currentTimeMillis() + "_" + originalFilename;

            byte[] bytes = validated.getBytes();
            String mimeType = validated.getDerivedMimeType();

            DriveFileDto result = googleDriveService.uploadFile(subfolder, gDriveFileName, mimeType, bytes);
            result.setCategory(category);
            result.setOriginalFileName(originalFilename);
            if (user != null) {
                result.setUploadedBy(user.getName());
            }

            log.info("☁️ [Cloud Storage] Successfully uploaded {} ({} bytes) into Google Drive subfolder: {}",
                    originalFilename, bytes.length, subfolder);

            return ResponseEntity.ok(ApiResponse.ok("File uploaded successfully to Google Drive", result));
        } catch (com.edusync.exception.BadRequestException e) {
            log.warn("⚠️ [File Upload] Rejected bad file: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("❌ [Cloud Storage] Upload failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to upload file to Google Drive: " + e.getMessage()));
        }
    }

    /**
     * Direct file download stream from Google Drive or local storage fallback.
     */
    @GetMapping("/files/download/{fileId}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String fileId) {
        if (fileId == null || fileId.contains("/") || fileId.contains("\\") || fileId.contains("..") || fileId.contains("\0")) {
            return ResponseEntity.badRequest().build();
        }

        try {
            byte[] fileBytes = googleDriveService.downloadFile(fileId);

            String rawFilename = fileId;
            if (rawFilename.startsWith("LOCAL_")) {
                rawFilename = rawFilename.substring(6);
            }
            if (rawFilename.length() > 33 && rawFilename.charAt(32) == '_') {
                rawFilename = rawFilename.substring(33);
            }
            String safeFilename = rawFilename.replaceAll("[\\r\\n\"'\\\\/;:<>*?|]", "_").trim();
            if (safeFilename.isBlank()) {
                safeFilename = "downloaded_file";
            }
            if (safeFilename.length() > 100) {
                safeFilename = safeFilename.substring(0, 100);
            }

            org.springframework.http.ContentDisposition contentDisposition = org.springframework.http.ContentDisposition
                    .attachment()
                    .filename(safeFilename, java.nio.charset.StandardCharsets.UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .headers(headers -> headers.setContentDisposition(contentDisposition))
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(fileBytes);
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid download request identifier [{}]: {}", fileId, e.getMessage());
            return ResponseEntity.badRequest().build();
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

        try {
            com.edusync.util.FileUploadValidator.ValidatedFile validated = com.edusync.util.FileUploadValidator.validateUpload(file, false);
            String originalFilename = validated.getSanitizedFilename();
            byte[] bytes = validated.getBytes();
            String mimeType = validated.getDerivedMimeType();

            String dept = user != null && user.getDepartment() != null ? user.getDepartment() : "General";
            String gDriveFileName = "MAT_" + dept + "_" + (semester != null ? semester + "_" : "") + System.currentTimeMillis() + "_" + originalFilename;

            DriveFileDto uploadResult = googleDriveService.uploadFile("Study-Materials", gDriveFileName, mimeType, bytes);

            String fileExt = validated.getExtension().toUpperCase();

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
        } catch (com.edusync.exception.BadRequestException e) {
            log.warn("⚠️ [Study Material Upload] Rejected bad file: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
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

        return ResponseEntity.ok(ApiResponse.ok("Study materials retrieved", materials));
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

        try {
            com.edusync.util.FileUploadValidator.ValidatedFile validated = com.edusync.util.FileUploadValidator.validateUpload(file, false);
            String originalFilename = validated.getSanitizedFilename();
            byte[] bytes = validated.getBytes();
            String mimeType = validated.getDerivedMimeType();

            String rollNumber = student != null && student.getRollNumber() != null ? student.getRollNumber() : "STUDENT";
            String gDriveFileName = "SUB_" + assignmentId + "_" + rollNumber + "_" + System.currentTimeMillis() + "_" + originalFilename;

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
        } catch (com.edusync.exception.BadRequestException e) {
            log.warn("⚠️ [Assignment Submit] Rejected bad file: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
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

        try {
            com.edusync.util.FileUploadValidator.ValidatedFile validated = com.edusync.util.FileUploadValidator.validateImageUpload(file);
            byte[] bytes = validated.getBytes();
            String mimeType = validated.getDerivedMimeType();
            String ext = validated.getExtension();

            Long userId = user != null ? user.getId() : 0L;
            String gDriveFileName = "AVATAR_USER_" + userId + "_" + System.currentTimeMillis() + "." + ext;

            DriveFileDto uploadResult = googleDriveService.uploadFile("Profile-Pictures", gDriveFileName, mimeType, bytes);

            if (user != null) {
                user.setProfilePic(uploadResult.getWebViewLink());
                userRepository.save(user);
            }

            return ResponseEntity.ok(ApiResponse.ok("Profile picture updated", uploadResult.getWebViewLink()));
        } catch (com.edusync.exception.BadRequestException e) {
            log.warn("⚠️ [Profile Photo Upload] Rejected bad file: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
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
