package com.edusync.controller;

import com.edusync.model.AuditLog;
import com.edusync.model.LeaveRequest;
import com.edusync.model.PlacementDrive;
import com.edusync.repository.AuditLogRepository;
import com.edusync.repository.LeaveRequestRepository;
import com.edusync.repository.PlacementDriveRepository;
import com.edusync.service.PdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Report REST Controller for Institutional PDF Reports generation.
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin
public class ReportController {

    private final PdfService pdfService;
    private final LeaveRequestRepository leaveRepo;
    private final PlacementDriveRepository driveRepo;
    private final AuditLogRepository auditRepo;

    @GetMapping(value = "/leaves/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getLeavesReportPdf(
            @RequestParam(required = false, defaultValue = "false") boolean download
    ) {
        List<LeaveRequest> leaves = leaveRepo.findAllByOrderByCreatedAtDesc();
        byte[] pdf = pdfService.generateLeaveReportPdf(leaves);

        String disposition = download ? "attachment; filename=\"TKMIT_Leave_Report.pdf\""
                                      : "inline; filename=\"TKMIT_Leave_Report.pdf\"";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(pdf);
    }

    @GetMapping(value = "/placement/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getPlacementReportPdf(
            @RequestParam(required = false, defaultValue = "false") boolean download
    ) {
        List<PlacementDrive> drives = driveRepo.findAll();
        byte[] pdf = pdfService.generatePlacementReportPdf(drives);

        String disposition = download ? "attachment; filename=\"TKMIT_Placement_Report.pdf\""
                                      : "inline; filename=\"TKMIT_Placement_Report.pdf\"";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(pdf);
    }

    @GetMapping(value = "/audit/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getAuditReportPdf(
            @RequestParam(required = false, defaultValue = "false") boolean download
    ) {
        List<AuditLog> logs = auditRepo.findTop50ByOrderByCreatedAtDesc();
        byte[] pdf = pdfService.generateAuditReportPdf(logs);

        String disposition = download ? "attachment; filename=\"TKMIT_Audit_Security_Report.pdf\""
                                      : "inline; filename=\"TKMIT_Audit_Security_Report.pdf\"";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(pdf);
    }
}
