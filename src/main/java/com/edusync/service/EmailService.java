package com.edusync.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Asynchronous Enterprise Email Notification Service.
 * Delivers responsive HTML emails for:
 * 1. Parent Academic Advisory Risk Alerts (<75% attendance / <40% CIE)
 * 2. Student Urgent Notices & Announcements
 * 3. Assignment Deadlines & Submissions
 * 4. Duty Leave Approval Status Updates
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${edusync.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${edusync.mail.from-address:notifications@tkmit.ac.in}")
    private String fromAddress;

    @Value("${edusync.mail.from-name:EduSync TKMIT Academic Portal}")
    private String fromName;

    // ─── 1. PARENT ACADEMIC ADVISORY EMAIL ────────────────────────────────────────

    @Async
    public void sendParentAcademicAlertEmail(
            String parentEmail,
            String studentName,
            String rollNumber,
            String department,
            double attendancePct,
            double seriesMarksPct,
            String advisorName,
            String remarks
    ) {
        if (parentEmail == null || parentEmail.isBlank()) return;

        String subject = "⚠️ Academic Advisory Notice: " + studentName + " (" + rollNumber + ") - KTU Criteria Review";

        String attBadge = attendancePct < 75.0
                ? "<span style='background:#fee2e2;color:#ef4444;padding:3px 8px;border-radius:4px;font-weight:700;'>🔴 " + attendancePct + "% (Below KTU 75% Condonation Limit)</span>"
                : "<span style='background:#dcfce7;color:#10b981;padding:3px 8px;border-radius:4px;font-weight:700;'>🟢 " + attendancePct + "% (Satisfactory)</span>";

        String marksBadge = seriesMarksPct < 40.0
                ? "<span style='background:#fee2e2;color:#ef4444;padding:3px 8px;border-radius:4px;font-weight:700;'>🔴 " + seriesMarksPct + "% (Below 40% Pass Criteria)</span>"
                : "<span style='background:#dcfce7;color:#10b981;padding:3px 8px;border-radius:4px;font-weight:700;'>🟢 " + seriesMarksPct + "% (Passing)</span>";

        String body = """
            <div style="font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;max-width:620px;margin:0 auto;border:1px solid #e2e8f0;border-radius:10px;overflow:hidden;background:#ffffff;">
                <div style="background:linear-gradient(135deg,#1e3a8a,#2563eb);color:#ffffff;padding:24px 28px;text-align:center;">
                    <h2 style="margin:0;font-size:22px;letter-spacing:0.5px;">TKM INSTITUTE OF TECHNOLOGY</h2>
                    <p style="margin:6px 0 0;font-size:13px;opacity:0.9;text-transform:uppercase;letter-spacing:1px;">Department of %s · Class Advisor Office</p>
                </div>
                <div style="padding:28px;color:#334155;line-height:1.6;">
                    <p style="font-size:15px;margin-top:0;">Dear Parent / Guardian,</p>
                    <p style="font-size:14px;">This is an official academic progress review notice regarding your ward <strong>%s</strong> (Roll No: <code>%s</code>).</p>
                    
                    <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:18px;margin:20px 0;">
                        <h4 style="margin:0 0 14px;color:#0f172a;font-size:14px;border-bottom:1px solid #e2e8f0;padding-bottom:8px;">📊 KTU Academic Regulations & Performance Summary</h4>
                        <table style="width:100%%;font-size:13px;border-collapse:collapse;">
                            <tr>
                                <td style="padding:6px 0;color:#64748b;">Overall Attendance:</td>
                                <td style="padding:6px 0;text-align:right;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding:6px 0;color:#64748b;">Series CIE Marks Average:</td>
                                <td style="padding:6px 0;text-align:right;">%s</td>
                            </tr>
                        </table>
                    </div>

                    <div style="background:#eff6ff;border-left:4px solid #2563eb;border-radius:4px;padding:14px 18px;margin-bottom:20px;">
                        <strong style="color:#1e3a8a;font-size:13px;display:block;margin-bottom:4px;">Class Advisor Advisory Notes:</strong>
                        <p style="margin:0;font-size:13px;color:#1e40af;">%s</p>
                    </div>

                    <p style="font-size:13px;color:#475569;">Please connect with the Class Advisor during advisory hours to discuss academic intervention strategies and attendance recovery.</p>
                    
                    <hr style="border:none;border-top:1px solid #e2e8f0;margin:24px 0 16px;">
                    <div style="font-size:12px;color:#64748b;">
                        <strong>%s</strong><br>
                        Class Advisor · Department of %s<br>
                        TKM Institute of Technology, Karuvelil, Kollam
                    </div>
                </div>
            </div>
        """.formatted(department, studentName, rollNumber, attBadge, marksBadge, remarks, advisorName, department);

        sendHtmlMail(parentEmail, subject, body);
    }

    // ─── 2. URGENT NOTICE / CIRCULAR EMAIL ────────────────────────────────────────

    @Async
    public void sendUrgentNoticeEmail(
            String toEmail,
            String studentName,
            String noticeTitle,
            String category,
            String content,
            String publisherName
    ) {
        if (toEmail == null || toEmail.isBlank()) return;

        String subject = "🚨 URGENT CIRCULAR: " + noticeTitle + " - TKMIT";

        String body = """
            <div style="font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;max-width:620px;margin:0 auto;border:1px solid #e2e8f0;border-radius:10px;overflow:hidden;background:#ffffff;">
                <div style="background:linear-gradient(135deg,#dc2626,#b91c1c);color:#ffffff;padding:22px 28px;text-align:center;">
                    <div style="display:inline-block;background:rgba(255,255,255,0.2);padding:3px 10px;border-radius:20px;font-size:11px;font-weight:700;margin-bottom:6px;letter-spacing:1px;">%s NOTICE</div>
                    <h2 style="margin:0;font-size:20px;">TKM INSTITUTE OF TECHNOLOGY</h2>
                </div>
                <div style="padding:28px;color:#334155;line-height:1.6;">
                    <p style="font-size:14px;margin-top:0;">Dear <strong>%s</strong>,</p>
                    <h3 style="color:#0f172a;font-size:17px;margin:12px 0 16px;">📢 %s</h3>
                    
                    <div style="background:#fef2f2;border:1px solid #fecaca;border-radius:8px;padding:18px;color:#991b1b;font-size:14px;line-height:1.6;margin-bottom:20px;">
                        %s
                    </div>

                    <p style="font-size:13px;color:#64748b;">Please log in to the <a href="http://localhost:8081" style="color:#2563eb;text-decoration:none;font-weight:600;">EduSync Student Portal</a> for more details and real-time updates.</p>
                    
                    <hr style="border:none;border-top:1px solid #e2e8f0;margin:24px 0 16px;">
                    <div style="font-size:12px;color:#64748b;">
                        Published by <strong>%s</strong> · TKM Institute of Technology
                    </div>
                </div>
            </div>
        """.formatted(category, studentName, noticeTitle, content, publisherName);

        sendHtmlMail(toEmail, subject, body);
    }

    // ─── 3. ASSIGNMENT DUE DATE EMAIL ─────────────────────────────────────────────

    @Async
    public void sendAssignmentNotificationEmail(
            String toEmail,
            String studentName,
            String subjectCode,
            String title,
            String description,
            String dueDate,
            int maxMarks,
            String facultyName
    ) {
        if (toEmail == null || toEmail.isBlank()) return;

        String subject = "📚 New Assignment: " + subjectCode + " - Due " + dueDate;

        String body = """
            <div style="font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;max-width:620px;margin:0 auto;border:1px solid #e2e8f0;border-radius:10px;overflow:hidden;background:#ffffff;">
                <div style="background:linear-gradient(135deg,#7c3aed,#6d28d9);color:#ffffff;padding:22px 28px;text-align:center;">
                    <h2 style="margin:0;font-size:20px;">NEW ACADEMIC ASSIGNMENT</h2>
                    <p style="margin:4px 0 0;font-size:13px;opacity:0.9;">%s</p>
                </div>
                <div style="padding:28px;color:#334155;line-height:1.6;">
                    <p style="font-size:14px;margin-top:0;">Hello <strong>%s</strong>,</p>
                    <h3 style="color:#0f172a;font-size:16px;margin:12px 0 14px;">📝 %s</h3>
                    
                    <div style="background:#f5f3ff;border:1px solid #ddd6fe;border-radius:8px;padding:16px;margin-bottom:20px;font-size:13px;">
                        <p style="margin:0 0 10px;color:#4c1d95;"><strong>Description:</strong> %s</p>
                        <table style="width:100%%;font-size:13px;">
                            <tr>
                                <td style="color:#6d28d9;"><strong>Submission Deadline:</strong></td>
                                <td style="text-align:right;"><span style="background:#7c3aed;color:#ffffff;padding:3px 10px;border-radius:12px;font-weight:700;">📅 %s</span></td>
                            </tr>
                            <tr>
                                <td style="color:#6d28d9;padding-top:6px;"><strong>Maximum CIE Marks:</strong></td>
                                <td style="text-align:right;padding-top:6px;font-weight:700;">%d Marks</td>
                            </tr>
                        </table>
                    </div>

                    <p style="font-size:13px;color:#64748b;">Ensure you upload your submission on EduSync before the deadline to avoid late mark penalties.</p>
                    
                    <hr style="border:none;border-top:1px solid #e2e8f0;margin:24px 0 16px;">
                    <div style="font-size:12px;color:#64748b;">
                        Assigned by <strong>%s</strong> · TKM Institute of Technology
                    </div>
                </div>
            </div>
        """.formatted(subjectCode, studentName, title, description, dueDate, maxMarks, facultyName);

        sendHtmlMail(toEmail, subject, body);
    }

    // ─── 4. DUTY LEAVE DECISION CERTIFICATE EMAIL ─────────────────────────────────

    @Async
    public void sendLeaveStatusEmail(
            String toEmail,
            String studentName,
            String leaveType,
            String eventName,
            String fromDate,
            String toDate,
            int noOfDays,
            String status,
            String decidedBy,
            String stage,
            String remark
    ) {
        if (toEmail == null || toEmail.isBlank()) return;

        boolean isApproved = "APPROVED".equalsIgnoreCase(status);
        String headerBg = isApproved ? "linear-gradient(135deg,#059669,#10b981)" : "linear-gradient(135deg,#dc2626,#ef4444)";
        String statusPill = isApproved
                ? "<span style='background:#d1fae5;color:#065f46;padding:4px 12px;border-radius:12px;font-weight:700;'>✅ APPROVED</span>"
                : "<span style='background:#fee2e2;color:#991b1b;padding:4px 12px;border-radius:12px;font-weight:700;'>❌ REJECTED</span>";

        String subject = (isApproved ? "✅ APPROVED: " : "❌ REJECTED: ") + leaveType + " Request for " + (eventName != null ? eventName : "Leave");

        String body = """
            <div style="font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;max-width:620px;margin:0 auto;border:1px solid #e2e8f0;border-radius:10px;overflow:hidden;background:#ffffff;">
                <div style="background:%s;color:#ffffff;padding:22px 28px;text-align:center;">
                    <h2 style="margin:0;font-size:20px;">DUTY LEAVE APPLICATION STATUS</h2>
                    <p style="margin:4px 0 0;font-size:13px;opacity:0.9;">TKM Institute of Technology Academic Workflow</p>
                </div>
                <div style="padding:28px;color:#334155;line-height:1.6;">
                    <p style="font-size:14px;margin-top:0;">Dear <strong>%s</strong>,</p>
                    <p style="font-size:14px;">Your request for <strong>%s</strong> has been evaluated:</p>
                    
                    <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:18px;margin:16px 0;font-size:13px;">
                        <table style="width:100%%;border-collapse:collapse;">
                            <tr>
                                <td style="padding:5px 0;color:#64748b;">Event / Purpose:</td>
                                <td style="padding:5px 0;text-align:right;font-weight:600;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding:5px 0;color:#64748b;">Leave Duration:</td>
                                <td style="padding:5px 0;text-align:right;font-weight:600;">%s to %s (%d Days)</td>
                            </tr>
                            <tr>
                                <td style="padding:5px 0;color:#64748b;">Sanction Status:</td>
                                <td style="padding:5px 0;text-align:right;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding:5px 0;color:#64748b;">Current Stage:</td>
                                <td style="padding:5px 0;text-align:right;font-weight:600;"><code>%s</code></td>
                            </tr>
                            <tr>
                                <td style="padding:5px 0;color:#64748b;">Evaluated By:</td>
                                <td style="padding:5px 0;text-align:right;font-weight:600;">%s</td>
                            </tr>
                        </table>
                    </div>

                    %s

                    <p style="font-size:13px;color:#64748b;">You can download your official Duty Leave PDF sanction receipt on the EduSync portal.</p>
                    
                    <hr style="border:none;border-top:1px solid #e2e8f0;margin:24px 0 16px;">
                    <div style="font-size:12px;color:#64748b;">
                        EduSync Automated Leave System · TKM Institute of Technology
                    </div>
                </div>
            </div>
        """.formatted(
                headerBg, studentName, leaveType,
                eventName != null ? eventName : "Official Activity",
                fromDate, toDate, noOfDays,
                statusPill, stage, decidedBy,
                remark != null && !remark.isBlank()
                        ? "<div style='background:#f1f5f9;border-left:3px solid #64748b;padding:10px 14px;font-size:13px;margin-bottom:16px;'><strong>Remarks:</strong> " + remark + "</div>"
                        : ""
        );

        sendHtmlMail(toEmail, subject, body);
    }

    // ─── INTERNAL HELPER ─────────────────────────────────────────────────────────

    private void sendHtmlMail(String toEmail, String subject, String htmlContent) {
        if (!mailEnabled) {
            log.info("[SIMULATED EMAIL] To: {} | Subject: {}", toEmail, subject);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(new InternetAddress(fromAddress, fromName));
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email dispatched successfully to {} [Subject: {}]", toEmail, subject);
        } catch (Exception e) {
            // Graceful log without breaking application execution
            log.warn("Could not dispatch live SMTP email to {} (simulating delivery): {}", toEmail, e.getMessage());
        }
    }
}
