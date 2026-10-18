package com.edusync.service.ai;

import com.edusync.dto.AiDto;
import com.edusync.model.PlacementDrive;
import com.edusync.repository.PlacementDriveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Deterministic knowledge base and expert guidance service for KTU curriculum,
 * university regulations, academic grading formulas, interviews, and technical cheat sheets.
 */
@Service
@RequiredArgsConstructor
public class KtuKnowledgeBaseService {

    private final PlacementDriveRepository driveRepo;

    /**
     * Resolves KTU rules, academic formulas, placement info, and career guides.
     */
    public AiDto.ChatResponse resolveKnowledgeQuery(String msg) {
        String lower = msg.toLowerCase();

        // 0. CURRENT DATE, DAY & TIME QUERIES (e.g. "what day is this", "what is today's date", "what time is it", "current day")
        if (lower.contains("what day") || lower.contains("which day") || lower.contains("today's date") ||
            lower.contains("what date") || lower.contains("what is the date") || lower.contains("current date") ||
            lower.contains("current time") || lower.contains("what time") || lower.contains("today date") ||
            lower.matches(".*\\b(day|date|time)\\s+is\\s+(it|today|this)\\b.*")) {

            ZoneId istZone = ZoneId.of("Asia/Kolkata");
            ZonedDateTime now = ZonedDateTime.now(istZone);
            String dayOfWeek = now.getDayOfWeek().toString().substring(0, 1) + now.getDayOfWeek().toString().substring(1).toLowerCase();
            String formattedDate = now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));
            String formattedTime = now.format(DateTimeFormatter.ofPattern("hh:mm a z"));

            String reply = String.format(
                    "### 📅 Campus Calendar & Time Information\n\n" +
                    "- **Today is:** **%s**\n" +
                    "- **Full Date:** **%s**\n" +
                    "- **Current IST Time:** `%s`\n\n" +
                    "**🎓 Academic Highlights:**\n" +
                    "- Standard college lecture hours run from **09:00 AM to 04:15 PM IST**.\n" +
                    "- To view your specific class schedule for today (%s), open the **🗓 Timetable** tab from your student dashboard!\n\n" +
                    "> 💡 **Tip:** You can ask me for today's timetable, current attendance, or KTU academic calendar deadlines anytime.",
                    dayOfWeek, formattedDate, formattedTime, dayOfWeek
            );

            return AiDto.ChatResponse.builder()
                    .reply(reply)
                    .intent("CALENDAR_DATE_TIME")
                    .suggestedActions(List.of("Check my attendance", "What is KTU 75% attendance rule?", "KTU Activity Points criteria"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 0.1 COURTESY / THANK YOU / GOODBYE / SYSTEM ABOUT QUERIES
        if (lower.matches(".*\\b(thank you|thanks|thx|thank u|appreciate it)\\b.*")) {
            return AiDto.ChatResponse.builder()
                    .reply("😊 **You are very welcome!**\n\nI'm always here to assist you with your **attendance, internal marks, KTU regulations, and placement preparation**. Let me know if there's anything else you need!")
                    .intent("COURTESY_THANK_YOU")
                    .suggestedActions(List.of("Check my attendance", "What are KTU Activity Points rules?", "Upcoming placement drives"))
                    .fromLocalLlm(false)
                    .build();
        }

        if (lower.matches(".*\\b(bye|goodbye|see you|cya|good night)\\b.*")) {
            return AiDto.ChatResponse.builder()
                    .reply("👋 **Goodbye! Have a productive and great day ahead!**\n\nFeel free to ask me anytime when you need academic assistance or portal help.")
                    .intent("COURTESY_GOODBYE")
                    .suggestedActions(List.of("Check my attendance", "What is KTU 75% attendance rule?"))
                    .fromLocalLlm(false)
                    .build();
        }

        if (lower.contains("who created you") || lower.contains("who made you") || lower.contains("about edusync") || lower.contains("what is edusync")) {
            return AiDto.ChatResponse.builder()
                    .reply("### 🤖 About EduSync Campus Copilot\n\n" +
                           "**EduSync** is the unified next-generation college digital platform for **TKM Institute of Technology (TKMIT)** affiliated with **APJ Abdul Kalam Technological University (KTU)**.\n\n" +
                           "**Core Capabilities:**\n" +
                           "- 📊 **Real-time Student ERP:** Live attendance tracking, internal marks (CIE), and automated shortage alerts.\n" +
                           "- 📝 **E-Governance:** Digital Duty Leave (OD) workflows, grievance redressal, and timetable schedules.\n" +
                           "- 💼 **AI Placement & ATS:** Resume keyword optimization, candidate ranking, and placement drive notifications.\n" +
                           "- 🎓 **RAG AI Campus Assistant:** Instant answers based on official KTU ordinances, syllabi, and academic manuals.")
                    .intent("SYSTEM_ABOUT")
                    .suggestedActions(List.of("Check my attendance", "What is KTU 75% attendance rule?", "KTU Activity Points criteria"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 1. MOOC COURSES & CREDIT TRANSFER RULES
        if (lower.contains("mooc") || lower.contains("nptel") || lower.contains("swayam") || lower.contains("online course") || lower.contains("credit transfer") || lower.contains("moocs")) {
            String reply =
                    "### 🎓 KTU Regulations for MOOC Courses (NPTEL / SWAYAM)\n\n" +
                    "- **Duration Requirement:** MOOC courses must have a minimum duration of **12 to 14 weeks**.\n" +
                    "- **Approval:** The course must be chosen from the approved list recommended by the concerned **Board of Studies** and approved by the Academic Council.\n" +
                    "- **B.Tech Honours & Minor:** Students can acquire up to **8 credits** by completing **two MOOC courses** (4 credits each) to fulfill the 15–20 credit requirement.\n" +
                    "- **Pass/Fail Grading:** If an approved MOOC course is certified on a Pass/Fail basis without numeric marks, the University assigns a letter grade of **'B+'**.\n" +
                    "- **MCA Programme:** 1st-year MCA students must complete at least **1 MOOC in S1** and **1 MOOC before S2 completion** from the published 10-course list.\n" +
                    "- **Internship Flexibility (2024 Scheme):** Students on long-term internships (4–6 months in S7/S8) can opt for MOOCs corresponding to Professional Electives (PE), Open Electives (OE), and Humanities (HMC) courses.";

            return AiDto.ChatResponse.builder()
                    .reply(reply)
                    .intent("MOOC_RULES")
                    .suggestedActions(List.of("B.Tech Honours & Minor credit rules", "Internship guidelines", "Activity points for MOOCs"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 2. KTU 2024 CURRICULUM, CREDITS, PBL & CHALLENGE COURSES
        if (lower.contains("2024 curriculum") || lower.contains("2024 scheme") || lower.contains("curriculum") || lower.contains("scheme") || lower.contains("challenge course") || lower.contains("pbl") || lower.contains("ects") || lower.contains("course code") || lower.contains("credit structure") || lower.contains("credits") || lower.contains("credit")) {
            String reply =
                    "### 📘 KTU B.Tech 2024 Curriculum Structure & Innovations\n\n" +
                    "**1. Total Credit Structure:**\n" +
                    "- **Regular B.Tech:** **170 Credits** (167 Academic + 3 Credits for 120 Activity Points).\n" +
                    "- **B.Tech with Minor / Honours:** **185 Credits** (170 + 15 coursework credits).\n" +
                    "- **B.Tech with Honours & Minor:** **200 Credits** (170 + 15 + 15).\n\n" +
                    "**2. Project-Based Learning (PBL - 16 Credits):**\n" +
                    "- Structured into *Collaborative-learning phase* and *Self-directed learning phase* (3 Lecture + 1 Project hr/week).\n" +
                    "- Project categories: Task Projects, Discipline Projects, Problem Projects, and Service-Learning Projects.\n\n" +
                    "**3. Challenge Courses (Credit-by-Examination):**\n" +
                    "- Allows students with Prior Learning (RPL) to earn course credits by passing the End Semester Examination (ESE) directly without regular CIA. Marks do not affect GPA.\n\n" +
                    "**4. Multilevel Courses (Bloom's Taxonomy):**\n" +
                    "- **Level 3 Courses (3 Credits):** Lower cognitive levels (Remember, Understand, Apply).\n" +
                    "- **Level 5 Courses (5 Credits):** Higher-order thinking (Analyze, Evaluate, Create) with 3 hrs class + 3 hrs self-study.\n\n" +
                    "**5. Course Coding Pattern [YYXXCSNN]:**\n" +
                    "- E.g., `UCMAT201` (University Core Maths S2), `PBCST604` (Project Based CS S6). `1 ECTS = 25 study hours` (`ACTS = ECTS/2`).";

            return AiDto.ChatResponse.builder()
                    .reply(reply)
                    .intent("CURRICULUM_2024")
                    .suggestedActions(List.of("What are the rules for MOOC courses?", "B.Tech Minor course distribution", "ECTS credit calculation"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 3. INTER-COLLEGE TRANSFER NORMS
        if (lower.contains("college transfer") || lower.contains("inter college") || lower.contains("inter-college") || lower.contains("transfer to another college") || lower.contains("college change") || lower.contains("transfer")) {
            String reply =
                    "### 🏫 KTU Inter-College Transfer Regulations (B.Tech 2019 Scheme)\n\n" +
                    "- **Eligibility Window:** Permitted only **just before the commencement of the 3rd Semester (S3)**.\n" +
                    "- **Permitted Transfers:** Between Govt/Govt-Aided colleges OR between Self-Financing colleges within sanctioned seat strength.\n" +
                    "- **Ineligible Categories:**\n" +
                    "  * ❌ Lateral Entry students\n" +
                    "  * ❌ Management Quota (Aided & Private SFC)\n" +
                    "  * ❌ NRI / PIO quota & TFW Scheme students\n" +
                    "  * ❌ Autonomous colleges (transfers to/from are prohibited)\n" +
                    "- **Transfer Fee:** **Rs. 3,000/-** payable to the University (Exempted for SC/ST students).\n\n" +
                    "**Procedure:** Submit request to Principal A -> Principal A recommends -> Principal B verifies vacancy & admits -> Relieved & admitted on KTU portal -> Remit fee.";

            return AiDto.ChatResponse.builder()
                    .reply(reply)
                    .intent("INTER_COLLEGE_TRANSFER")
                    .suggestedActions(List.of("University Migration rules", "Promotion credit requirements", "Duty leave rules"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 4. UNIVERSITY MIGRATION RULES
        if (lower.contains("migration") || lower.contains("migrate from other university") || lower.contains("transfer from another university")) {
            String reply =
                    "### 🌐 KTU Norms for Migration from Other Universities\n\n" +
                    "- **Semester Limit:** Permitted only **up to the 5th semester** of B.Tech / B.Arch (or half duration for other programmes).\n" +
                    "- **Institutional Recognition:** Parent university must have AICTE/UGC/AIU approval and an active bipartite MoU/agreement with KTU.\n" +
                    "- **Syllabus Match:** Minimum **80% syllabus equivalence** and **>60% practical syllabus coverage** assessed by the Board of Studies. Transitory courses may be prescribed.\n" +
                    "- **Fee Structure:**\n" +
                    "  * Application Processing Fee: **Rs. 5,000/-**\n" +
                    "  * Migration Fee: **Rs. 20,000/-** (Non-refundable)\n" +
                    "  *(Students from non-deemed Kerala universities prior to KTU Act are exempted)*.\n" +
                    "- **Documents Required:** NOC from college, Migration Certificate from parent university, Character Certificate, and attested mark lists from 10th standard onwards.";

            return AiDto.ChatResponse.builder()
                    .reply(reply)
                    .intent("UNIVERSITY_MIGRATION")
                    .suggestedActions(List.of("Inter-college transfer norms", "Credit requirement for S5", "Break of study rules"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 5. CGPA TO PERCENTAGE CONVERSION & GRADING
        if (lower.contains("cgpa") || lower.contains("sgpa") || lower.contains("gpa") || lower.contains("percentage formula") || lower.contains("percentage") || lower.contains("grade point") || lower.contains("convert cgpa") || lower.contains("grading") || lower.contains("grade calculation") || lower.contains("cgpa to percentage") || lower.contains("sgpa to percentage")) {
            String reply =
                    "### 🎯 KTU Official CGPA / SGPA to Percentage Conversion Formulas\n\n" +
                    "| Regulation / Scheme | Official Percentage Formula |\n" +
                    "|---|---|\n" +
                    "| **B.Tech 2024 Scheme** | **`Percentage Marks (% Marks) = 10 × CGPA`** |\n" +
                    "| **B.Tech 2019 Scheme, MCA 2020, MBA 2020** | **`Percentage Marks (% Marks) = (10 × CGPA) - 2.5`** |\n" +
                    "| **B.Tech 2015 Scheme, B.Arch 2016, BHMCT 2018** | **`Percentage Marks (% Marks) = (10 × CGPA) - 3.75`** |\n\n" +
                    "**Degree Classifications:**\n" +
                    "- **First Class with Distinction:** CGPA **≥ 8.0**\n" +
                    "- **First Class:** CGPA **≥ 6.5**\n\n" +
                    "**10-Point Grading Scale:** `S (10, ≥90%)`, `A+ (9.0, 85-89%)`, `A (8.5, 80-84%)`, `B+ (8.0, 75-79%)`, `B (7.5, 70-74%)`, `C+ (7.0, 65-69%)`, `C (6.5, 60-64%)`, `D (6.0, 55-59%)`, `P (5.5, 50-54%)`, `F (0, <50% aggregate or <40% ESE)`.";

            return AiDto.ChatResponse.builder()
                    .reply(reply)
                    .intent("PERCENTAGE_CONVERSION")
                    .suggestedActions(List.of("CIE passing minimum", "Credit requirements for S5 and S7", "Check my attendance"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 6. HIGHER SEMESTER CREDIT PROMOTION GATES
        if (lower.contains("credit requirement") || lower.contains("credits to register") || lower.contains("promotion rule") || lower.contains("year back") || lower.contains("credits for s5") || lower.contains("credits for s7")) {
            String reply =
                    "### 📊 KTU Minimum Cumulative Credit Requirements for Higher Semesters\n\n" +
                    "**1. B.Tech 2024 Scheme (R 7.30):**\n" +
                    "- **To S5:** Minimum **18 Credits** from S1 & S2 (Total curriculum S1-S2: 44 credits).\n" +
                    "- **To S7:** Minimum **37 Credits** from S1 to S4 (Lateral Entry: **15 Credits** from S3 & S4).\n" +
                    "- **To S3, S4, S6, S8:** *Not Insisted* (Minimum 40% average attendance required).\n\n" +
                    "**2. B.Tech 2019 Scheme (R 6.17):**\n" +
                    "- **To S5:** Minimum **21 Credits** from S1 & S2.\n" +
                    "- **To S7:** Minimum **47 Credits** from S1 to S4 (Lateral Entry: **9 Credits** from S3-S4).\n\n" +
                    "> 💡 *Credit Relaxation Policy (2024):* Allows graduation with 160 credits (157 academic + 3 activity credits) with up to 10 credits shortfall.";

            return AiDto.ChatResponse.builder()
                    .reply(reply)
                    .intent("CREDIT_PROMOTION")
                    .suggestedActions(List.of("What is KTU attendance rule?", "Calculate CGPA to percentage", "Rules for MOOC courses"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 7. STUDENT ACTIVITY POINTS & BREAK OF STUDY
        if (lower.contains("activity point") || lower.contains("activity points") || lower.contains("ncc") || lower.contains("nss") || lower.contains("break of study") || lower.contains("hackathon")) {
            String reply =
                    "### 🏆 KTU Student Activity Points Handbook (2024 Scheme)\n\n" +
                    "**Mandatory Activity Points for 3 Credits (1 credit from each group):**\n" +
                    "- **Regular Students:** **120 Points** (Minimum **40 Points from each of Group I, II, and III**).\n" +
                    "- **Lateral Entry Students:** **90 Points** (Minimum **30 Points** from each Group).\n" +
                    "- **PwD Students:** **60 Points** (Minimum **20 Points** from each Group).\n\n" +
                    "**Points Breakdown by Groups:**\n" +
                    "- **Group I (Sports, Arts, NSS/NCC & Social):**\n" +
                    "  * Sports/Arts: College (1–5 pts), Zonal (5–10), State (10–20), National/International (20–40 pts)\n" +
                    "  * NCC 'C' Cert (30 pts), 'B' Cert (20 pts) | NSS 2-Year Volunteer (30 pts)\n" +
                    "  * Blood Donation (5 pts/donation) | Tree Planting (5 pts) | Driving License (5 pts) | Swimming (5 pts)\n" +
                    "- **Group II (Technical, FOSS, Standardized Tests):**\n" +
                    "  * Tech Fest / Professional Societies (IEEE, ACM): 5–15 pts | Paper Presentation: 5–20 pts\n" +
                    "  * Standardized Tests: TOEFL (20–30 pts), IELTS (20–30 pts), GRE (20–30 pts), GATE (20–30 pts), CAT (20–30 pts)\n" +
                    "  * Short-Term Internship (2 weeks): 10 pts\n" +
                    "- **Group III (Startups, Patents, Hackathons, Projects):**\n" +
                    "  * National Hackathons (SIH, KAVACH): 30–40 pts | International Hackathons: 30–40 pts\n" +
                    "  * Registered Startup (30 pts) | Patent Filed/Granted (20–40 pts) | Prototype Tested (40 pts)\n" +
                    "  * Research Publication (SCI/Scopus Q1–Q2: 40 pts, Q3–Q4: 25 pts) | Approved Skilling (1 pt/hour up to 40 pts).\n\n" +
                    "**Break of Study:** Max 1–2 years for serious medical grounds, student startup (after S4), or campus placement (>6 LPA CTC).";

            return AiDto.ChatResponse.builder()
                    .reply(reply)
                    .intent("ACTIVITY_POINTS_BREAK_STUDY")
                    .suggestedActions(List.of("What are the rules for MOOC courses?", "Duty Leave attendance benefit", "KTU 2024 Curriculum"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 8. DUTY LEAVE & OD LETTER RULES
        if (lower.contains("duty leave") || lower.contains("od letter") || lower.contains("on duty") || lower.contains("od") || lower.contains("apply leave") || lower.contains("how to apply") || lower.contains("leave application")) {
            String reply =
                    "### 📝 TKMIT Duty Leave & On-Duty (OD) Workflow\n\n" +
                    "Students representing the college in technical symposiums, hackathons, sports, or cultural fests can claim Duty Leave (OD).\n\n" +
                    "**Step-by-Step Approval Chain:**\n" +
                    "1. **Submission:** Apply via EduSync portal with event name, organizer, and dates (upload certificate / invitation if available).\n" +
                    "2. **Faculty Advisor Review:** Verifies academic standing and recommends the leave.\n" +
                    "3. **HOD Clearance:** Department Head reviews and sanctions attendance credit.\n" +
                    "4. **Principal Approval:** Required for multi-day inter-college or national level events.\n" +
                    "5. **Official PDF Certificate:** Once approved, an official digital signed certificate is auto-generated with QR verification.\n\n" +
                    "> ⚠️ **Benefit:** Up to **10% attendance relaxation** for University/State representation (overall attendance must be ≥60%) and **5%** for organizing college co-curricular events.";

            return AiDto.ChatResponse.builder()
                    .reply(reply)
                    .intent("LEAVE_APPLY_INFO")
                    .suggestedActions(List.of("Check my leave status", "What is KTU attendance rule?", "Internal marks calculation"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 9. KTU ATTENDANCE & CONDONATION RULES
        if (lower.contains("attendance") || lower.contains("attendence") || lower.contains("attend") ||
            lower.contains("condonation") || lower.contains("condone") || lower.contains("75%") ||
            lower.contains("shortage") || lower.contains("medical leave") || lower.contains("menstrual") ||
            lower.contains("bunk") || lower.contains("fe grade") || lower.contains("eligibility") ||
            lower.matches(".*\\b(attendance|attendence|absent|present|shortage|bunking)\\b.*")) {
            String reply =
                    "### 🏛 APJ Abdul Kalam Technological University (KTU) Attendance Rules\n\n" +
                    "- **Minimum Regular Requirement:** **75%** attendance is compulsory in each registered course to write the End Semester Examination (ESE).\n" +
                    "- **Special Relaxations (2024 Regulations - R 6.1):**\n" +
                    "  * 🌸 **Menstrual Leave:** **2% attendance relaxation** for eligible students (lowers requirement to **73%**).\n" +
                    "  * ♿ **PWD Students:** **5% attendance relaxation** (lowers requirement to **70%**).\n" +
                    "- **Medical Condonation (60% to 74% - R 6.2):**\n" +
                    "  * Principal can condone attendance shortage down to **60%** on genuine medical grounds (hospitalization/accident) with medical fitness certificates (max 2 semesters across degree).\n" +
                    "- **Below 60% Attendance:** Awarded **'FE' grade** (Failed due to Eligibility) and student must repeat the course.\n" +
                    "- **Minimum Attendance for Semester Promotion (R 7.32):** Student must maintain at least **40% average attendance** to advance to the next semester.";

            return AiDto.ChatResponse.builder()
                    .reply(reply)
                    .intent("ATTENDANCE_RULES")
                    .suggestedActions(List.of("Check my current attendance", "How do I apply for Duty Leave?", "Internal marks weightage"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 10. INTERNAL MARKS & ASSESSMENT CALCULATION
        if (lower.contains("internal") || lower.contains("internal marks") || lower.contains("series exam") || lower.contains("calculate marks") || lower.contains("cie")) {
            String reply =
                    "### 📊 KTU Continuous Internal Evaluation (CIE) Calculation (50 Marks Total)\n\n" +
                    "Internal marks for B.Tech theory courses are evaluated out of **50 marks** as follows:\n\n" +
                    "| Component | Maximum Marks | Details |\n" +
                    "|---|---|---|\n" +
                    "| **Series Exam I & II** | **25 Marks** | Average of Series 1 & 2 exams (conducted for 50 marks each, scaled to 25) |\n" +
                    "| **Assignments / Micro-projects** | **15 Marks** | 2 graded assignments + tutorial problems |\n" +
                    "| **Attendance & Regularity** | **10 Marks** | Graded based on attendance (≥90%: 10m, 85–89%: 8m, 80–84%: 6m, 75–79%: 4m) |\n\n" +
                    "> **Passing Criteria:** Minimum **40% (40/100)** in ESE and **50% aggregate (CIE + ESE)** to pass.\n" +
                    "> **CIE Normalization:** CIE percentage cannot exceed ESE percentage by more than **30%** (e.g. if ESE is 40%, maximum normalized CIE is 70%).";

            return AiDto.ChatResponse.builder()
                    .reply(reply)
                    .intent("INTERNAL_MARKS")
                    .suggestedActions(List.of("Check my attendance", "Placement drives eligibility", "Grievance redressal process"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 11. PLACEMENT DRIVES & ELIGIBILITY
        if (lower.contains("placement") || lower.contains("drive") || lower.contains("company") || lower.contains("salary") || lower.contains("package") || lower.contains("eligible") || lower.contains("infosys") || lower.contains("tcs")) {
            List<PlacementDrive> upcoming = driveRepo.findByStatusOrderByDriveDateDesc(PlacementDrive.DriveStatus.UPCOMING);
            List<PlacementDrive> ongoing = driveRepo.findByStatusOrderByDriveDateDesc(PlacementDrive.DriveStatus.ONGOING);

            StringBuilder sb = new StringBuilder();
            sb.append("### 💼 Campus Placement Drives & Eligibility\n\n");

            if (!ongoing.isEmpty() || !upcoming.isEmpty()) {
                sb.append("**Active & Upcoming Campus Drives:**\n");
                for (PlacementDrive d : ongoing) {
                    sb.append(String.format("- 🟢 **%s** — *%s* | **%.1f LPA** | Min CGPA: **%.1f** | Drive: %s\n",
                            d.getCompanyName(), d.getRole(), d.getPackageLpa() != null ? d.getPackageLpa() : 0.0,
                            d.getMinCgpa() != null ? d.getMinCgpa() : 6.0, d.getDriveDate()));
                }
                for (PlacementDrive d : upcoming) {
                    sb.append(String.format("- 🟡 **%s** — *%s* | **%.1f LPA** | Min CGPA: **%.1f** | Drive: %s\n",
                            d.getCompanyName(), d.getRole(), d.getPackageLpa() != null ? d.getPackageLpa() : 0.0,
                            d.getMinCgpa() != null ? d.getMinCgpa() : 6.0, d.getDriveDate()));
                }
            } else {
                sb.append("Current recruitment drives include **Infosys (SE Trainee — 4.2 LPA)**, **TCS Digital (7.5 LPA)**, and **Cognizant (4.0 LPA)**.\n");
            }

            sb.append("\n**General Campus Placement Criteria:**\n");
            sb.append("- Minimum **6.5+ CGPA** (7.0+ for premium tier companies).\n");
            sb.append("- **No active backlogs** at the time of the drive.\n");
            sb.append("- Strong foundation in Data Structures, Java/Python, Database SQL, and Core Engineering fundamentals.\n\n");
            sb.append("💡 *Tip: Run our built-in **AI Resume Screening** tool to check your ATS Match Score before applying!*");

            return AiDto.ChatResponse.builder()
                    .reply(sb.toString())
                    .intent("PLACEMENT_INFO")
                    .suggestedActions(List.of("Run AI Resume Review", "Check my attendance", "How to apply for Duty Leave?"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 12. GRIEVANCE REDRESSAL
        if (lower.contains("grievance") || lower.contains("complaint") || lower.contains("ragging") || lower.contains("issue") || lower.contains("harassment")) {
            String reply =
                    "### 🛡️ TKMIT Student Grievance Redressal Cell\n\n" +
                    "EduSync features a secure and confidential grievance redressal mechanism.\n\n" +
                    "- **Categories:** Academic, Examination, Infrastructure, Hostel, Fee, or Anti-Ragging.\n" +
                    "- **Target Authorities:** You can route complaints directly to your **Faculty Advisor**, **Head of Department (HOD)**, or **Principal**.\n" +
                    "- **Anonymous Option:** You can choose to keep your identity strictly anonymous.\n" +
                    "- **Resolution Timeline:** The college administration is committed to resolving submitted grievances within **3–5 working days**.\n" +
                    "- **Anti-Ragging:** Ragging is a criminal non-bailable offense leading to immediate dismissal.";

            return AiDto.ChatResponse.builder()
                    .reply(reply)
                    .intent("GRIEVANCE_INFO")
                    .suggestedActions(List.of("File a grievance", "Check my active leaves", "What is KTU attendance rule?"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 13. OTHER KTU PROGRAMMES (M.TECH, MBA, MCA, B.ARCH, BHMCT, B.DES)
        if (lower.contains("m.tech") || lower.contains("mtech") || lower.contains("mba") ||
            lower.contains("mca") || lower.contains("b.arch") || lower.contains("barch") ||
            lower.contains("bhmct") || lower.contains("b.des") || lower.contains("b.design") ||
            lower.contains("pg programme") || lower.contains("postgraduate")) {
            String reply =
                    "### 🎓 KTU Postgraduate & Professional Programme Credits & Norms\n\n" +
                    "| Programme | Total Credits | Semesters | Max Duration | Special Notes |\n" +
                    "|---|---|---|---|---|\n" +
                    "| **M.Tech (2015 Scheme)** | 64–68 Credits | 4 semesters | 6 semesters | 85% attendance required (condonation to 70%), Dissertation 150 marks (S3: 50m, S4: 100m) |\n" +
                    "| **MBA (2020 Scheme)** | 102 Credits | 4 semesters | 8 semesters | Foundation course pass mark: 75%, Min 40% ESE + 50% aggregate |\n" +
                    "| **MBA Trimester (2015 Scheme)** | 115 Credits | 6 trimesters | 4 years | Industry placement in 5th trimester |\n" +
                    "| **MCA (2-Year Scheme)** | 86 Credits | 4 semesters | 8 semesters | 2 MOOC courses mandatory in Year 1 |\n" +
                    "| **MCA (5-Year Integrated)** | 214 Credits | 10 semesters | 14 semesters | — |\n" +
                    "| **B.Arch (2016 Scheme)** | 225 Credits | 10 semesters | 14 semesters | Design Jury: 40% + CIE: 60%; 100-day practical training in S7 |\n" +
                    "| **BHMCT (2018 Scheme)** | 197 Credits | 8 semesters | 12 semesters | 20-week Industrial Training in S6; % = 10×CGPA − 3.75 |\n" +
                    "| **B.Des (2019 Scheme)** | 162 Credits | 8 semesters | 12 semesters | Design Studio: CIE 60% + Jury 40%; 60-day training in S6 |\n\n" +
                    "> 💡 *MOOC courses are applicable to MCA 1st year (2 courses in S1 & S2) and to B.Tech Honours/Minor students.*";

            return AiDto.ChatResponse.builder()
                    .reply(reply)
                    .intent("OTHER_PROGRAMMES")
                    .suggestedActions(List.of("KTU attendance rules", "CGPA to percentage formula", "University migration rules"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 14. KTU CONTACTS & SUPPORT DESK
        if (lower.contains("contact") || lower.contains("phone") || lower.contains("email") ||
            lower.contains("helpdesk") || lower.contains("support") || lower.contains("ktu office") ||
            lower.contains("vice chancellor") || lower.contains("dean")) {
            String reply =
                    "### 📞 KTU Official Contacts & E-Governance Support Desk\n\n" +
                    "| Role | Contact | Phone |\n" +
                    "|---|---|---|\n" +
                    "| **Vice Chancellor** | vc@ktu.edu.in | 0471-2598222 |\n" +
                    "| **Dean (Academics)** | deanacademics@ktu.edu.in | 0471-2785622 |\n" +
                    "| **E-Governance Help Desk** | support@ktu.edu.in | 0471-2593120 / 2593128 / 2590029 |\n" +
                    "| **General Queries** | university@ktu.edu.in | — |\n" +
                    "| **Academic Matters** | academics@ktu.edu.in | — |\n\n" +
                    "**For TKMIT campus issues, contact:**\n" +
                    "- Academic Section: academicofficer@tkmit.ac.in\n" +
                    "- Placement Cell: placement@tkmit.ac.in\n" +
                    "- Principal's Office: principal@tkmit.ac.in";

            return AiDto.ChatResponse.builder()
                    .reply(reply)
                    .intent("KTU_CONTACTS")
                    .suggestedActions(List.of("File a grievance", "Attendance rules", "Placement drives"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 15. EQUIVALENCY CERTIFICATE
        if (lower.contains("equivalency") || lower.contains("equivalence certificate") || lower.contains("certificate fee")) {
            String reply =
                    "### 📄 KTU Equivalency Certificate\n\n" +
                    "Students who migrated from another university to KTU and need an official equivalency certificate:\n\n" +
                    "- **First Applicant Fee:** Rs. 6,000/-\n" +
                    "- **Subsequent Applicants (same course / batch):** Rs. 1,000/- each\n" +
                    "- **Assessment:** Conducted by the concerned Board of Studies based on ≥80% syllabus equivalence match.\n" +
                    "- **Transitory Courses:** If equivalence is partial, additional transitory courses may be prescribed to fulfill credit requirements.\n\n" +
                    "> Contact the KTU Equivalency Cell at: academics@ktu.edu.in";

            return AiDto.ChatResponse.builder()
                    .reply(reply)
                    .intent("EQUIVALENCY_CERTIFICATE")
                    .suggestedActions(List.of("University migration fee", "Inter-college transfer rules", "KTU contacts"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 16. HR INTERVIEWS & BEHAVIORAL QUESTIONS
        if (lower.contains("tell me about yourself") || lower.contains("hr interview") || lower.contains("behavioral question") ||
            lower.contains("star method") || lower.contains("why should we hire you") || lower.contains("weakness") || lower.contains("interview tip")) {
            String reply =
                    "### 💼 Campus Placement & HR Interview Master Guide (Claude & Gemini Framework)\n\n" +
                    "#### 1. How to answer *\"Tell Me About Yourself\"* (Present → Past → Future Framework):\n" +
                    "- **Present (30s):** State your name, college (TKMIT), department (e.g. CSE), current GPA, and core technical passion (e.g., *\"I am a full-stack engineer passionate about scalable backend systems and distributed databases...\"*).\n" +
                    "- **Past (40s):** Highlight 1–2 standout accomplishments: your best major/mini project, hackathon win, or internship experience with measurable outcomes.\n" +
                    "- **Future (20s):** Connect why this specific company and role is the natural next step in your career journey.\n\n" +
                    "#### 2. The STAR Method for Behavioral Questions:\n" +
                    "- **S (Situation):** Set the context and challenge you faced.\n" +
                    "- **T (Task):** Define your specific responsibility.\n" +
                    "- **A (Action):** Detail the exact technical decisions and leadership steps YOU took.\n" +
                    "- **R (Result):** Conclude with quantifiable business or technical impact (*\"Reduced response time by 40%\"*).\n\n" +
                    "#### 3. Top HR Questions Cheat Sheet:\n" +
                    "- *\"What is your greatest weakness?\"* → Pick a genuine technical skill you were weak at, but immediately explain the active steps you took to master it.\n" +
                    "- *\"Where do you see yourself in 5 years?\"* → Focus on technical mastery, mentorship, and driving architecture.";

            return AiDto.ChatResponse.builder()
                    .reply(reply)
                    .intent("INTERVIEW_GUIDE")
                    .suggestedActions(List.of("Binary search & time complexity", "ACID properties in DBMS", "How to score 9+ CGPA in KTU"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 17. DATA STRUCTURES & ALGORITHMS (DSA) GUIDE
        if (lower.contains("binary search") || lower.contains("dsa") || lower.contains("dynamic programming") ||
            lower.contains("time complexity") || lower.contains("big o") || lower.contains("sorting algorithm") ||
            lower.contains("merge sort") || lower.contains("quick sort")) {
            String reply =
                    "### ⚡ Data Structures & Algorithms (DSA) Essentials\n\n" +
                    "#### 🔍 Binary Search Algorithm:\n" +
                    "- **Prerequisite:** Array / list **MUST be sorted** in ascending or descending order.\n" +
                    "- **Mechanism:** Compare the target value with the middle element (`mid = low + (high - low)/2`). If target < mid, eliminate right half; if target > mid, eliminate left half.\n" +
                    "- **Time Complexity:** **`O(log N)`** (Worst & Average) | **`O(1)`** (Best)\n" +
                    "- **Space Complexity:** **`O(1)`** (Iterative) | **`O(log N)`** (Recursive)\n\n" +
                    "#### 📊 Common Algorithm Complexities:\n" +
                    "| Algorithm | Best Time | Average Time | Worst Time | Space |\n" +
                    "|---|---|---|---|---|\n" +
                    "| **Binary Search** | `O(1)` | `O(log N)` | `O(log N)` | `O(1)` |\n" +
                    "| **Merge Sort** | `O(N log N)` | `O(N log N)` | `O(N log N)` | `O(N)` |\n" +
                    "| **Quick Sort** | `O(N log N)` | `O(N log N)` | `O(N²)` | `O(log N)` |\n" +
                    "| **Hash Table Lookup** | `O(1)` | `O(1)` | `O(N)` | `O(N)` |\n\n" +
                    "#### 💡 Dynamic Programming (DP) Blueprint:\n" +
                    "- Identify overlapping subproblems and optimal substructure.\n" +
                    "- Use **Top-Down Memoization** (Recursion + Cache) or **Bottom-Up Tabulation** (Iterative DP Table).";

            return AiDto.ChatResponse.builder()
                    .reply(reply)
                    .intent("DSA_GUIDE")
                    .suggestedActions(List.of("ACID properties in DBMS", "STAR method for interviews", "Check my attendance"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 18. CORE COMPUTER SCIENCE FUNDAMENTALS (OS, DBMS, NETWORKS)
        if (lower.contains("acid") || lower.contains("deadlock") || lower.contains("paging") ||
            lower.contains("osi layer") || lower.contains("tcp vs udp") || lower.contains("normalization") ||
            lower.contains("oop concept") || lower.contains("polymorphism")) {
            String reply =
                    "### 🖥️ Core Computer Science Fundamentals Cheat Sheet\n\n" +
                    "#### 1. ACID Properties (DBMS):\n" +
                    "- **A (Atomicity):** \"All or nothing\" — if any part of transaction fails, the entire transaction is rolled back.\n" +
                    "- **C (Consistency):** Database transitions only from one valid state to another, preserving integrity constraints.\n" +
                    "- **I (Isolation):** Concurrent transactions execute independently without reading uncommitted dirty data.\n" +
                    "- **D (Durability):** Once committed, data changes permanently survive power failures or crashes.\n\n" +
                    "#### 2. Deadlock (Operating Systems):\n" +
                    "- **4 Necessary Coffman Conditions:** 1. Mutual Exclusion, 2. Hold & Wait, 3. No Preemption, 4. Circular Wait.\n" +
                    "- **Avoidance:** Dijkstra's Banker's Algorithm.\n\n" +
                    "#### 3. OSI 7-Layer Model:\n" +
                    "1. Physical → 2. Data Link → 3. Network (IP) → 4. Transport (TCP/UDP) → 5. Session → 6. Presentation → 7. Application (HTTP/DNS).";

            return AiDto.ChatResponse.builder()
                    .reply(reply)
                    .intent("CORE_CS_GUIDE")
                    .suggestedActions(List.of("Binary search & time complexity", "STAR method for interviews", "How to score 9+ CGPA in KTU"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 19. 9+ CGPA & KTU ACADEMIC STRATEGY
        if (lower.contains("score 9") || lower.contains("9+ cgpa") || lower.contains("study tip") || lower.contains("exam strategy") || lower.contains("how to study")) {
            String reply =
                    "### 🏆 Strategy to Maintain 9+ CGPA & B.Tech Honours in KTU\n\n" +
                    "1. **Maximize Continuous Internal Evaluation (CIE - 50 Marks):**\n" +
                    "   - Series Exams 1 & 2 contribute to 60% of internal assessment. Target **45+/50** in both.\n" +
                    "   - Submit all micro-assignments and lab writeups on time for full 10 marks.\n" +
                    "2. **Master Previous Year Questions (PYQs):**\n" +
                    "   - Solve the last **3–5 years KTU question papers**. More than 70% of exam patterns follow repeated university question blueprints.\n" +
                    "3. **Structured Answer Presentation:**\n" +
                    "   - Draw neat block diagrams with captions for 10-mark questions.\n" +
                    "   - Underline key formulas and highlight final numerical solutions with units.\n" +
                    "4. **KTU Honours Degree Requirements (R 16.8):**\n" +
                    "   - Maintain CGPA ≥ 8.0 with **zero backlogs** across all semesters and complete 15 additional credits.";

            return AiDto.ChatResponse.builder()
                    .reply(reply)
                    .intent("STUDY_STRATEGY")
                    .suggestedActions(List.of("What is STAR method?", "Binary search & time complexity", "Check my CGPA"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 20. ENGINEERING BRANCH COMPARISONS & COURSE SELECTION ADVISORY
        if (lower.contains("computer science") && (lower.contains("civil") || lower.contains("mechanical") || lower.contains("ece") || lower.contains("better") || lower.contains("best")) ||
            lower.contains("which is the best course") || lower.contains("which branch is best") || lower.contains("cs vs civil") || lower.contains("best engineering branch")) {
            String reply =
                    "### 🎓 Engineering Branch Comparison: Computer Science (CSE) vs. Civil Engineering (CE)\n\n" +
                    "Both **Computer Science** and **Civil Engineering** are excellent branches with distinct career trajectories, skill requirements, and industry scopes.\n\n" +
                    "| Parameter | 💻 Computer Science & Engineering (CSE) | 🏗️ Civil Engineering (CE) |\n" +
                    "|---|---|---|\n" +
                    "| **Primary Focus** | Software development, AI/ML, Cloud computing, Algorithms, Cyber security | Infrastructure, Structural design, Bridges, Transportation, Geotechnical engineering |\n" +
                    "| **Job Market & Demand** | Massive private sector demand, high volume of campus recruitment drives | Stable core engineering demand in construction, government infrastructure, and PSU sectors |\n" +
                    "| **Starting Packages** | **₹4.5 LPA – ₹25+ LPA** (High growth trajectory) | **₹3.5 LPA – ₹10 LPA** (Long-term growth with professional licensure) |\n" +
                    "| **Government Opportunities** | ISRO, DRDO, NIC, Cyber cells | IES (UPSC), CPWD, PWD, NHAI, Railways, PSUs (ONGC, NTPC) |\n" +
                    "| **Work Environment** | Tech hubs, remote work, product startups | On-site project management, design offices, fieldwork |\n\n" +
                    "#### 💡 Which Should You Choose?\n" +
                    "- **Choose Computer Science if:** You enjoy programming, problem-solving, rapid technology evolution, remote work flexibility, and high campus placement opportunities.\n" +
                    "- **Choose Civil Engineering if:** You are passionate about physical infrastructure, sustainable urban planning, on-site project execution, or targeting prestigious government engineering jobs (IES/UPSC).";

            return AiDto.ChatResponse.builder()
                    .reply(reply)
                    .intent("BRANCH_COMPARISON")
                    .suggestedActions(List.of("Upcoming placement drives", "How to score 9+ CGPA in KTU", "KTU B.Tech Honours requirements"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 21. KTU ACTIVITY POINTS RULES & DEGREE REQUIREMENTS
        if (lower.contains("activity point") || lower.contains("activity points")) {
            String reply =
                    "### 🏆 KTU Activity Points Regulations & Degree Requirements\n\n" +
                    "Under APJ Abdul Kalam Technological University (KTU) regulations, earning Activity Points is **mandatory** for the award of the B.Tech degree:\n\n" +
                    "**1. Total Points Required for Degree:**\n" +
                    "- **Regular 4-Year B.Tech Students:** Minimum **100 Activity Points** (or **120 Points** for B.Tech Honours / 2024 scheme).\n" +
                    "- **Lateral Entry Students (joining in S3):** Minimum **75 Activity Points** (or **90 Points** for Honours).\n\n" +
                    "**2. Activity Categories & Maximum Point Caps:**\n" +
                    "1. **National Initiatives (NCC / NSS):** Up to **60 points** (e.g., NSS 2-year volunteer certificate = 60 pts).\n" +
                    "2. **Sports & Games:** Up to **60 points** (College level = 10 pts, Zonal = 20 pts, Inter-University / National = 40–60 pts).\n" +
                    "3. **Cultural Activities:** Up to **40 points** (University Arts Fest winner = 30 pts, College level = 10 pts).\n" +
                    "4. **Professional Initiatives:** Up to **50 points** (IEEE, CSI, ISTE membership = 10 pts, Paper publication in Scopus/IEEE = 30–40 pts, Hackathon prizes = 30–50 pts).\n" +
                    "5. **Entrepreneurship & Innovation (IEDC):** Up to **50 points** (Startup ideation/incubation, patent filing, approved MOOC certificates).\n\n" +
                    "> 💡 **Submission:** Upload certificates to the KTU Student Portal. Your Faculty Advisor (FA) verifies and approves them semester by semester.";

            return AiDto.ChatResponse.builder()
                    .reply(reply)
                    .intent("ACTIVITY_POINTS_RULES")
                    .suggestedActions(List.of("What is Condonation rule?", "How do I apply for Duty Leave?", "What is KTU 75% attendance rule?"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 22. ATTENDANCE CONDONATION REGULATIONS
        if (lower.contains("condonation") || lower.contains("condone")) {
            String reply =
                    "### 🏥 KTU Attendance Condonation Regulations (Regulation R 6.2)\n\n" +
                    "Condonation in KTU is granted strictly for **Attendance Shortage** due to genuine medical reasons or authorized college duties:\n\n" +
                    "- **Eligible Range:** Attendance between **60% and 74%** in a course.\n" +
                    "- **Maximum Limit:** Permitted for a **maximum of 2 semesters** throughout the entire 4-year B.Tech program.\n" +
                    "- **Critical Floor (< 60%):** Attendance below 60% **CANNOT be condoned** under any circumstances. The student is awarded an **'FE' (Failed due to Eligibility)** grade and must repeat the course.\n" +
                    "- **Approval Chain:** Student submits Medical Certificate + Application → Recommended by Faculty Advisor (FA) & HOD → Approved by College Principal → Forwarded to KTU Portal.\n\n" +
                    "> ⚠️ **Important Note:** Condonation is strictly for attendance shortage. Academic subject backlogs (F grades) are cleared through regular supplementary examinations, never through condonation.";

            return AiDto.ChatResponse.builder()
                    .reply(reply)
                    .intent("CONDONATION_RULES")
                    .suggestedActions(List.of("What is KTU 75% attendance rule?", "How do I apply for Duty Leave?", "KTU Activity Points criteria"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 23. SFA / FACULTY ADVISOR ROLE
        if (lower.contains("sfa") || lower.contains("faculty advisor") || lower.contains("student faculty advisor")) {
            String reply =
                    "### 👨‍🏫 SFA (Senior Faculty Advisor) & Faculty Advisor System in KTU\n\n" +
                    "Every student admitted to KTU is attached to a designated **Faculty Advisor (FA)** and **Senior Faculty Advisor (SFA)**:\n\n" +
                    "- **Cohort Mentorship:** Each Faculty Advisor mentors a batch of approximately **30 students** from Semester 1 through graduation.\n" +
                    "- **Key Responsibilities:**\n" +
                    "  1. **Course Registration:** Approves semester course registrations, professional electives, and Minor/Honours course selections.\n" +
                    "  2. **Academic Monitoring:** Tracks Continuous Internal Evaluation (CIE) marks and attendance records.\n" +
                    "  3. **Duty Leaves & Condonations:** First approving authority for On-Duty (OD) applications and Medical Condonation requests.\n" +
                    "  4. **Parent Advisory:** Communicates progress to parents and organizes semester Class Committee / Parent-Teacher meetings.\n" +
                    "  5. **Activity Points:** Verifies and validates student activity certificates uploaded to the KTU portal.";

            return AiDto.ChatResponse.builder()
                    .reply(reply)
                    .intent("SFA_INFO")
                    .suggestedActions(List.of("What are the rules for KTU activity points?", "Check my attendance", "How to apply for Duty Leave?"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 24. DISCRETE MATHEMATICS STUDY GUIDE
        if (lower.contains("discrete math") || lower.contains("discrete mathematics") || lower.contains("mat203") || lower.contains("mat206") || lower.contains("cst201")) {
            String reply =
                    "### 📐 KTU Discrete Mathematical Structures (MAT203 / CST201) Study Guide\n\n" +
                    "Discrete Mathematics is a foundational core course for Computer Science & IT engineering. Here is how to master each module:\n\n" +
                    "**Module-wise Breakdown & Key Topics:**\n" +
                    "- **Module 1 (Logic & Proofs):** Propositional logic, Truth tables, Logical equivalences, Predicates & Quantifiers, Rules of inference, Proof techniques (Direct, Contradiction, Induction).\n" +
                    "- **Module 2 (Sets, Relations & Functions):** Set operations, Equivalence relations & Partitions, Partial order relations (Posets & Hasse diagrams), Lattices, Pigeonhole Principle.\n" +
                    "- **Module 3 (Algebraic Structures):** Semigroups, Monoids, Groups, Subgroups, Cosets, Lagrange's Theorem, Homomorphisms, Rings & Fields.\n" +
                    "- **Module 4 (Combinatorics & Recurrence):** Permutations & Combinations, Inclusion-Exclusion principle, Generating functions, Solving Linear Recurrence Relations.\n" +
                    "- **Module 5 (Graph Theory):** Basic graph definitions, Isomorphism, Eulerian & Hamiltonian paths, Trees, Minimum Spanning Trees (Prim's/Kruskal's), Planar graphs.\n\n" +
                    "**📚 Recommended Textbooks:**\n" +
                    "1. *Kenneth H. Rosen*, \"Discrete Mathematics and Its Applications\" (Standard KTU Reference).\n" +
                    "2. *J.P. Tremblay & R. Manohar*, \"Discrete Mathematical Structures with Applications to Computer Science\".\n\n" +
                    "💡 **Exam Tip:** Solve the last 5 years' KTU question papers—induction proofs, Hasse diagrams, and solving recurrence relations frequently repeat in Part B!";

            return AiDto.ChatResponse.builder()
                    .reply(reply)
                    .intent("DISCRETE_MATH_GUIDE")
                    .suggestedActions(List.of("Binary search & time complexity", "ACID properties in DBMS", "How to score 9+ CGPA in KTU"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 25. KTU 75% ATTENDANCE REGULATION (GENERAL)
        if (lower.contains("75% attendance") || lower.contains("attendance rule") || lower.contains("minimum attendance") || lower.contains("attendance") || lower.contains("attendence") || lower.contains("attend")) {
            String reply =
                    "### 📊 KTU 75% Mandatory Attendance Regulation\n\n" +
                    "Under APJ Abdul Kalam Technological University (KTU) regulations:\n\n" +
                    "- **Mandatory Requirement:** Minimum **75% attendance** in each registered subject is required to appear for the End Semester Examination (ESE).\n" +
                    "- **Condonation Zone (60% to 74%):** Attendance shortage in this bracket can be condoned by the Principal on genuine medical grounds (hospitalization) or institutional representation. Permitted for a maximum of 2 semesters in a degree.\n" +
                    "- **Critical Shortage (< 60%):** Results in an **'FE' (Failed due to Eligibility)** grade. The student is disqualified from writing the exam and must repeat the course when offered next.\n" +
                    "- **Duty Leave (OD):** Students representing the college in sports, hackathons, or university events can receive up to 10% attendance relaxation via approved Duty Leave.";

            return AiDto.ChatResponse.builder()
                    .reply(reply)
                    .intent("KTU_ATTENDANCE_RULE")
                    .suggestedActions(List.of("What is Condonation rule?", "How do I apply for Duty Leave?", "KTU Activity Points criteria"))
                    .fromLocalLlm(false)
                    .build();
        }

        // 21. GENERAL SMART FALLBACK
        String reply = String.format(
                "### 🤖 EduSync Campus AI Assistant\n\n" +
                "I understand you're asking about: *\"%s\"*\n\n" +
                "Here are the key areas I can assist you with right now:\n" +
                "1. 🎓 **MOOC & Credit Transfer:** 12-14 week rules, NPTEL/SWAYAM, B+ grade mapping, and Honours/Minor credits.\n" +
                "2. 📘 **KTU 2024 Curriculum:** 170 credits, Project-Based Learning (PBL), Challenge Courses, and Multilevel Levels 3 & 5.\n" +
                "3. 🏫 **College Transfer & Migration:** Inter-college transfer (before S3, Rs. 3,000) and University migration (up to S5, 80%% syllabus match).\n" +
                "4. 🎯 **CGPA & Grade Calculations:** Conversion formulas (`10 × CGPA - 2.5`), CIE 1:2 weightage, and normalization.\n" +
                "5. 📊 **Attendance & Duty Leave:** 75%% regular, 60%% condonation, OD certificate workflow, and placement drives.\n\n" +
                "Feel free to click any of the suggested topics below or ask a specific question!",
                msg
        );

        return AiDto.ChatResponse.builder()
                .reply(reply)
                .intent("GENERAL_QUERY")
                .suggestedActions(List.of("Rules for MOOC courses", "How to convert CGPA to percentage?", "What is KTU 2024 credit structure?", "Inter-college transfer norms"))
                .fromLocalLlm(false)
                .build();
    }
}
