package com.edusync.controller;

import com.edusync.dto.AiDto;
import com.edusync.dto.AppDto;
import com.edusync.model.PlacementApplication;
import com.edusync.model.PlacementDrive;
import com.edusync.repository.PlacementApplicationRepository;
import com.edusync.repository.PlacementDriveRepository;
import com.edusync.security.JwtUtil;
import com.edusync.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller exposing Local AI Campus Assistant and Resume Screening ATS.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin
public class AiController {

    private final AiService aiService;
    private final JwtUtil jwtUtil;
    private final PlacementDriveRepository driveRepo;
    private final PlacementApplicationRepository appRepo;

    /**
     * Campus AI Chatbot endpoint for students, faculty, and visitors.
     */
    @PostMapping("/chat")
    public ResponseEntity<AppDto.ApiResponse<AiDto.ChatResponse>> chat(
            @RequestBody AiDto.ChatRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Long userId = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                userId = jwtUtil.extractUserId(authHeader.substring(7));
            } catch (Exception ignored) {
            }
        }
        AiDto.ChatResponse response = aiService.chat(request, userId);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("AI response generated", response));
    }

    /**
     * AI Resume Screening ATS endpoint for placement officers.
     */
    @PostMapping("/screen-resumes")
    public ResponseEntity<AppDto.ApiResponse<AiDto.BatchScreenResponse>> screenResumes(
            @RequestBody AiDto.ResumeScreenRequest request
    ) {
        AiDto.BatchScreenResponse response = aiService.screenCandidatesForDrive(request);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Batch AI resume screening complete", response));
    }

    /**
     * AI Resume Review & Optimization Analysis for students.
     */
    @PostMapping("/analyze-resume")
    public ResponseEntity<AppDto.ApiResponse<AiDto.ResumeAnalyzeResponse>> analyzeResume(
            @RequestBody AiDto.ResumeAnalyzeRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Long userId = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                userId = jwtUtil.extractUserId(authHeader.substring(7));
            } catch (Exception ignored) {
            }
        }
        AiDto.ResumeAnalyzeResponse response = aiService.analyzeStudentResume(request, userId);
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Resume ATS analysis complete", response));
    }

    /**
     * Run AI screening on all applicants for an existing placement drive and update their AI scores.
     */
    @GetMapping("/screen-drive/{driveId}")
    public ResponseEntity<AppDto.ApiResponse<AiDto.BatchScreenResponse>> screenDriveApplicants(
            @PathVariable Long driveId
    ) {
        PlacementDrive drive = driveRepo.findById(driveId)
                .orElseThrow(() -> new RuntimeException("Placement drive not found: " + driveId));

        List<PlacementApplication> applications = appRepo.findByDrive(drive);
        List<AiDto.CandidateInput> candidates = new ArrayList<>();

        for (PlacementApplication app : applications) {
            if (app.getStudent() != null) {
                candidates.add(AiDto.CandidateInput.builder()
                        .studentId(app.getStudent().getId())
                        .name(app.getStudent().getName())
                        .email(app.getStudent().getEmail())
                        .department(app.getStudent().getDepartment() != null ? app.getStudent().getDepartment() : "CSE")
                        .cgpa(app.getCgpa() != null ? app.getCgpa() : 7.8)
                        .resumeText(String.format("Student %s in department %s. Skills: Java, Spring Boot, MySQL, REST APIs, Git, Problem Solving.",
                                app.getStudent().getName(), app.getStudent().getDepartment()))
                        .build());
            }
        }

        AiDto.ResumeScreenRequest screenReq = AiDto.ResumeScreenRequest.builder()
                .driveId(drive.getId())
                .companyName(drive.getCompanyName())
                .jobRole(drive.getRole())
                .minCgpa(drive.getMinCgpa())
                .eligibleBranches(drive.getEligibleBranches())
                .candidates(candidates)
                .build();

        AiDto.BatchScreenResponse result = aiService.screenCandidatesForDrive(screenReq);

        // Update AI scores in database for matching applicants
        for (AiDto.ResumeScreenResult r : result.getRankedResults()) {
            for (PlacementApplication app : applications) {
                if (app.getStudent() != null && app.getStudent().getId().equals(r.getStudentId())) {
                    app.setAiScore(r.getAtsScore());
                    appRepo.save(app);
                }
            }
        }

        return ResponseEntity.ok(AppDto.ApiResponse.ok("Drive applicants screened successfully", result));
    }

    private final com.edusync.service.GoogleDriveService googleDriveService;

    /**
     * Get status of Google Drive cloud synchronization.
     */
    @GetMapping("/drive-status")
    public ResponseEntity<AppDto.ApiResponse<java.util.Map<String, Object>>> getDriveStatus() {
        java.util.Map<String, Object> status = new java.util.LinkedHashMap<>();
        status.put("configured", googleDriveService.isConfigured());
        status.put("folderId", googleDriveService.getFolderId());
        status.put("folderUrl", "https://drive.google.com/drive/folders/" + googleDriveService.getFolderId());
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Google Drive Status", status));
    }

    /**
     * Manually trigger bi-directional sync with Google Drive (supports both GET and POST).
     */
    @RequestMapping(value = "/sync-drive", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<AppDto.ApiResponse<java.util.Map<String, Object>>> triggerDriveSync() {
        java.util.Map<String, Object> syncResult = googleDriveService.syncAll();
        return ResponseEntity.ok(AppDto.ApiResponse.ok("Google Drive Sync Result", syncResult));
    }
}
