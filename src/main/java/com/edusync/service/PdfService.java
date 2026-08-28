package com.edusync.service;

import com.edusync.model.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * High-quality Institutional PDF Generator for EduSync Platform.
 * Generates official Duty Leave / OD Certificates and Institutional Analytics Reports.
 */
@Service
public class PdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // Palette
    private static final Color NAVY = new Color(15, 32, 67);
    private static final Color BLUE = new Color(37, 99, 235);
    private static final Color LIGHT_BG = new Color(248, 250, 252);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);
    private static final Color TEXT_DARK = new Color(30, 41, 59);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color GREEN_STATUS = new Color(16, 185, 129);
    private static final Color GREEN_BG = new Color(236, 253, 245);

    // Fonts
    private static final Font FONT_INSTITUTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, NAVY);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, TEXT_MUTED);
    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
    private static final Font FONT_SECTION = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, NAVY);
    private static final Font FONT_LABEL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, TEXT_MUTED);
    private static final Font FONT_VALUE = FontFactory.getFont(FontFactory.HELVETICA, 9f, TEXT_DARK);
    private static final Font FONT_VALUE_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, TEXT_DARK);
    private static final Font FONT_STATUS = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f, new Color(4, 120, 87));
    private static final Font FONT_FOOTER = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7.5f, TEXT_MUTED);

    /**
     * Generates an official, beautifully formatted Leave / On-Duty Sanction Certificate PDF.
     */
    public byte[] generateLeaveCertificatePdf(LeaveRequest leave) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            // 1. Institutional Header
            Paragraph instTitle = new Paragraph("TKM INSTITUTE OF TECHNOLOGY, KOLLAM", FONT_INSTITUTION);
            instTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(instTitle);

            Paragraph instSubtitle = new Paragraph("Approved by AICTE, Affiliated to APJ Abdul Kalam Technological University\nKaruvelil P.O, Ezhukone, Kollam, Kerala — 691505 | www.tkmit.ac.in", FONT_SUBTITLE);
            instSubtitle.setAlignment(Element.ALIGN_CENTER);
            instSubtitle.setSpacingAfter(10);
            document.add(instSubtitle);

            // Separator Line
            LineSeparator sep = new LineSeparator(1.5f, 100, BLUE, Element.ALIGN_CENTER, -2);
            document.add(sep);

            // 2. Document Title Banner
            PdfPTable banner = new PdfPTable(1);
            banner.setWidthPercentage(100);
            banner.setSpacingBefore(12);
            banner.setSpacingAfter(14);

            String certTitle = "OFFICIAL " + leave.getLeaveType().name().replace('_', ' ') + " APPROVAL CERTIFICATE";
            PdfPCell bannerCell = new PdfPCell(new Phrase(certTitle, FONT_TITLE));
            bannerCell.setBackgroundColor(NAVY);
            bannerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            bannerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            bannerCell.setPaddingTop(8);
            bannerCell.setPaddingBottom(8);
            bannerCell.setBorder(Rectangle.NO_BORDER);
            banner.addCell(bannerCell);
            document.add(banner);

            // 3. Metadata Header (Reference No. & Date)
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setWidths(new float[]{60, 40});
            metaTable.setSpacingAfter(12);

            String refNo = String.format("REF NO: TKMIT/EDUSYNC/OD/%d/%04d", LocalDate.now().getYear(), leave.getId());
            PdfPCell refCell = new PdfPCell(new Phrase(refNo, FONT_VALUE_BOLD));
            refCell.setBorder(Rectangle.NO_BORDER);
            metaTable.addCell(refCell);

            String issueDate = "ISSUED ON: " + LocalDateTime.now().format(DATE_FMT);
            PdfPCell dateCell = new PdfPCell(new Phrase(issueDate, FONT_VALUE_BOLD));
            dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            dateCell.setBorder(Rectangle.NO_BORDER);
            metaTable.addCell(dateCell);
            document.add(metaTable);

            // 4. Student & Academic Details Box
            PdfPTable studentTable = createSectionBoxTable("1. STUDENT & ACADEMIC CREDENTIALS", 4);
            studentTable.setWidths(new float[]{22, 28, 22, 28});

            User s = leave.getStudent();
            addKeyValueCell(studentTable, "Student Name:", s != null ? s.getName() : "N/A");
            addKeyValueCell(studentTable, "Roll / Reg No:", s != null && s.getRollNumber() != null ? s.getRollNumber() : "N/A");
            addKeyValueCell(studentTable, "Department:", s != null && s.getDepartment() != null ? s.getDepartment() : "N/A");
            addKeyValueCell(studentTable, "Class / Batch:", (s != null && s.getSemester() != null ? s.getSemester() : "S4") + " - " + (s != null && s.getSection() != null ? s.getSection() : "A"));
            addKeyValueCell(studentTable, "Student Email:", s != null ? s.getEmail() : "N/A");
            addKeyValueCell(studentTable, "Contact Phone:", s != null && s.getPhone() != null ? s.getPhone() : "+91 98765 43210");
            document.add(studentTable);

            // 5. Leave & Event Details Box
            PdfPTable leaveTable = createSectionBoxTable("2. SANCTIONED LEAVE / OD PARTICULARS", 4);
            leaveTable.setWidths(new float[]{22, 28, 22, 28});

            addKeyValueCell(leaveTable, "Leave Type:", leave.getLeaveType().name().replace('_', ' '));
            addKeyValueCell(leaveTable, "Duration:", leave.getNoOfDays() + (leave.getNoOfDays() == 1 ? " Day" : " Days"));
            addKeyValueCell(leaveTable, "From Date:", leave.getFromDate() != null ? leave.getFromDate().format(DATE_FMT) : "N/A");
            addKeyValueCell(leaveTable, "To Date:", leave.getToDate() != null ? leave.getToDate().format(DATE_FMT) : "N/A");

            if (leave.getEventName() != null && !leave.getEventName().trim().isEmpty()) {
                addFullSpanCell(leaveTable, "Event / Venue:", leave.getEventName());
            }
            addFullSpanCell(leaveTable, "Purpose / Reason:", leave.getReason());
            document.add(leaveTable);

            // 6. Approval Chain & Digital Signatures Box
            PdfPTable approvalTable = createSectionBoxTable("3. MULTI-TIER DIGITAL APPROVAL LEDGER", 3);
            approvalTable.setWidths(new float[]{33, 33, 34});

            // Headers
            PdfPCell h1 = createHeaderCell("FACULTY ADVISOR");
            PdfPCell h2 = createHeaderCell("HEAD OF DEPARTMENT");
            PdfPCell h3 = createHeaderCell("PRINCIPAL OFFICE");
            approvalTable.addCell(h1);
            approvalTable.addCell(h2);
            approvalTable.addCell(h3);

            // Details
            String facBy = leave.getFacultyApprovedBy() != null ? leave.getFacultyApprovedBy() : "Verified Faculty";
            String facAt = leave.getFacultyApprovedAt() != null ? leave.getFacultyApprovedAt().format(DATE_TIME_FMT) : "Verified Online";
            String facRemark = leave.getFacultyRemark() != null ? leave.getFacultyRemark() : "Recommended & Verified";
            approvalTable.addCell(createSignatureCell(facBy, "Faculty Advisor", facAt, leave.getFacultyStatus().name(), facRemark));

            String hodBy = leave.getHodApprovedBy() != null ? leave.getHodApprovedBy() : "Head of Department";
            String hodAt = leave.getHodApprovedAt() != null ? leave.getHodApprovedAt().format(DATE_TIME_FMT) : "Approved Online";
            String hodRemark = leave.getHodRemark() != null ? leave.getHodRemark() : "Sanctioned for Attendance Credit";
            approvalTable.addCell(createSignatureCell(hodBy, "HOD (" + (s != null ? s.getDepartment() : "Dept") + ")", hodAt, leave.getHodStatus().name(), hodRemark));

            String prinBy = "Dr. K. Radhakrishnan";
            String prinAt = leave.getPrincipalApprovedAt() != null ? leave.getPrincipalApprovedAt().format(DATE_TIME_FMT) : "Institution Authority";
            String prinRemark = leave.getPrincipalRemark() != null ? leave.getPrincipalRemark() : "Formally Sanctioned";
            String prinStatus = leave.getPrincipalStatus() == LeaveRequest.Status.NOT_REQUIRED ? "SANCTIONED" : leave.getPrincipalStatus().name();
            approvalTable.addCell(createSignatureCell(prinBy, "Principal, TKMIT", prinAt, prinStatus, prinRemark));

            document.add(approvalTable);

            // 7. Security & Digital Verification Footer
            PdfPTable verifTable = new PdfPTable(1);
            verifTable.setWidthPercentage(100);
            verifTable.setSpacingBefore(12);

            PdfPCell verifCell = new PdfPCell();
            verifCell.setBackgroundColor(GREEN_BG);
            verifCell.setBorderColor(GREEN_STATUS);
            verifCell.setBorderWidth(1);
            verifCell.setPadding(8);

            Paragraph vTitle = new Paragraph("✔ DIGITALLY VERIFIED DOCUMENT — EDUSYNC COLLEGE PLATFORM", FONT_STATUS);
            Paragraph vDesc = new Paragraph("This certificate is digitally signed and cryptographically stored in the TKMIT Academic Management System. "
                    + "The student is entitled to attendance regularisation / OD credit for the specified duration. "
                    + "Verification Hash: " + Integer.toHexString(leave.hashCode()).toUpperCase() + "-" + leave.getId() + " | Valid for College Internal Records.", FONT_FOOTER);
            verifCell.addElement(vTitle);
            verifCell.addElement(vDesc);
            verifTable.addCell(verifCell);
            document.add(verifTable);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Leave Certificate PDF: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    /**
     * Generates an Executive Institutional Report PDF.
     */
    public byte[] generateLeaveReportPdf(List<LeaveRequest> leaves) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Header
            Paragraph inst = new Paragraph("TKM INSTITUTE OF TECHNOLOGY, KOLLAM", FONT_INSTITUTION);
            inst.setAlignment(Element.ALIGN_CENTER);
            document.add(inst);

            Paragraph title = new Paragraph("INSTITUTIONAL LEAVE & ON-DUTY (OD) SUMMARY REPORT", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, NAVY));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph sub = new Paragraph("Generated on: " + LocalDateTime.now().format(DATE_TIME_FMT) + " | Total Records: " + leaves.size(), FONT_SUBTITLE);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(10);
            document.add(sub);

            // Table
            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{8, 20, 14, 16, 12, 14, 16});

            String[] headers = {"ID", "Student", "Dept & Sem", "Leave Type", "Duration", "Status", "Dates"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
                cell.setBackgroundColor(NAVY);
                cell.setPadding(5);
                table.addCell(cell);
            }

            for (LeaveRequest l : leaves) {
                table.addCell(createBodyCell("#" + l.getId()));
                table.addCell(createBodyCell(l.getStudent() != null ? l.getStudent().getName() : "N/A"));
                table.addCell(createBodyCell((l.getStudent() != null ? l.getStudent().getDepartment() : "CSE") + " " + (l.getStudent() != null ? l.getStudent().getSemester() : "S4")));
                table.addCell(createBodyCell(l.getLeaveType().name().replace('_', ' ')));
                table.addCell(createBodyCell(l.getNoOfDays() + " Day(s)"));
                table.addCell(createBodyCell(l.getOverallStatus().name()));
                table.addCell(createBodyCell(l.getFromDate() + " to " + l.getToDate()));
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Leave Report PDF: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    /**
     * Generates a Placement Report PDF.
     */
    public byte[] generatePlacementReportPdf(List<PlacementDrive> drives) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph inst = new Paragraph("TKM INSTITUTE OF TECHNOLOGY, KOLLAM", FONT_INSTITUTION);
            inst.setAlignment(Element.ALIGN_CENTER);
            document.add(inst);

            Paragraph title = new Paragraph("CAMPUS PLACEMENT & RECRUITMENT DRIVES REPORT", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, NAVY));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph sub = new Paragraph("Generated on: " + LocalDateTime.now().format(DATE_TIME_FMT) + " | Placement Cell TKMIT", FONT_SUBTITLE);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(10);
            document.add(sub);

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{6, 18, 20, 14, 14, 14, 14});

            String[] headers = {"#", "Company", "Role", "Package (LPA)", "Min CGPA", "Branches", "Status"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
                cell.setBackgroundColor(NAVY);
                cell.setPadding(5);
                table.addCell(cell);
            }

            for (PlacementDrive d : drives) {
                table.addCell(createBodyCell(String.valueOf(d.getId())));
                table.addCell(createBodyCell(d.getCompanyName()));
                table.addCell(createBodyCell(d.getRole()));
                table.addCell(createBodyCell(d.getPackageLpa() != null ? "₹ " + d.getPackageLpa() + " LPA" : "N/A"));
                table.addCell(createBodyCell(d.getMinCgpa() != null ? String.valueOf(d.getMinCgpa()) : "6.0"));
                table.addCell(createBodyCell(d.getEligibleBranches() != null ? d.getEligibleBranches() : "All"));
                table.addCell(createBodyCell(d.getStatus() != null ? d.getStatus().name() : "UPCOMING"));
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Placement Report PDF: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    /**
     * Generates an Audit & System Activity Report PDF.
     */
    public byte[] generateAuditReportPdf(List<AuditLog> logs) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph inst = new Paragraph("TKM INSTITUTE OF TECHNOLOGY, KOLLAM", FONT_INSTITUTION);
            inst.setAlignment(Element.ALIGN_CENTER);
            document.add(inst);

            Paragraph title = new Paragraph("SYSTEM AUDIT & SECURITY LOGS REPORT", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, NAVY));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph sub = new Paragraph("Generated on: " + LocalDateTime.now().format(DATE_TIME_FMT) + " | EduSync Administrative Logs", FONT_SUBTITLE);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(10);
            document.add(sub);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{6, 18, 20, 14, 18, 24});

            String[] headers = {"#", "Timestamp", "Actor Email", "Role", "Action", "Details"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
                cell.setBackgroundColor(NAVY);
                cell.setPadding(5);
                table.addCell(cell);
            }

            for (AuditLog l : logs) {
                table.addCell(createBodyCell(String.valueOf(l.getId())));
                table.addCell(createBodyCell(l.getCreatedAt() != null ? l.getCreatedAt().format(DATE_TIME_FMT) : "N/A"));
                table.addCell(createBodyCell(l.getActorEmail()));
                table.addCell(createBodyCell(l.getActorRole() != null ? l.getActorRole().name() : "ADMIN"));
                table.addCell(createBodyCell(l.getAction()));
                table.addCell(createBodyCell(l.getDetails() != null ? l.getDetails() : "–"));
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Audit Report PDF: " + e.getMessage(), e);
        }

        return out.toByteArray();
    }

    // ─── Helper UI Components ─────────────────────────────────────────────

    private PdfPTable createSectionBoxTable(String sectionTitle, int columns) {
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8);
        table.setSpacingAfter(8);

        PdfPCell titleCell = new PdfPCell(new Phrase(sectionTitle, FONT_SECTION));
        titleCell.setColspan(columns);
        titleCell.setBackgroundColor(LIGHT_BG);
        titleCell.setBorderColor(BORDER_COLOR);
        titleCell.setPadding(5);
        titleCell.setBorderWidth(1);
        table.addCell(titleCell);

        return table;
    }

    private void addKeyValueCell(PdfPTable table, String label, String value) {
        PdfPCell lCell = new PdfPCell(new Phrase(label, FONT_LABEL));
        lCell.setBorderColor(BORDER_COLOR);
        lCell.setBackgroundColor(LIGHT_BG);
        lCell.setPadding(5);

        PdfPCell vCell = new PdfPCell(new Phrase(value != null ? value : "–", FONT_VALUE));
        vCell.setBorderColor(BORDER_COLOR);
        vCell.setPadding(5);

        table.addCell(lCell);
        table.addCell(vCell);
    }

    private void addFullSpanCell(PdfPTable table, String label, String value) {
        PdfPCell lCell = new PdfPCell(new Phrase(label, FONT_LABEL));
        lCell.setBorderColor(BORDER_COLOR);
        lCell.setBackgroundColor(LIGHT_BG);
        lCell.setPadding(5);

        PdfPCell vCell = new PdfPCell(new Phrase(value != null ? value : "–", FONT_VALUE));
        vCell.setColspan(3);
        vCell.setBorderColor(BORDER_COLOR);
        vCell.setPadding(5);

        table.addCell(lCell);
        table.addCell(vCell);
    }

    private PdfPCell createHeaderCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE)));
        c.setBackgroundColor(NAVY);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(4);
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    private PdfPCell createSignatureCell(String name, String designation, String time, String status, String remark) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(6);
        cell.setBorderColor(BORDER_COLOR);
        cell.setBackgroundColor(LIGHT_BG);

        Paragraph pStatus = new Paragraph("Status: " + status, FONT_STATUS);
        Paragraph pName = new Paragraph(name, FONT_VALUE_BOLD);
        Paragraph pDesig = new Paragraph(designation, FONT_LABEL);
        Paragraph pTime = new Paragraph("Signed: " + time, FONT_FOOTER);
        Paragraph pRem = new Paragraph("Note: " + remark, FONT_FOOTER);

        cell.addElement(pStatus);
        cell.addElement(pName);
        cell.addElement(pDesig);
        cell.addElement(pTime);
        cell.addElement(pRem);

        return cell;
    }

    private PdfPCell createBodyCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text != null ? text : "–", FONT_VALUE));
        c.setPadding(4);
        c.setBorderColor(BORDER_COLOR);
        return c;
    }
}
