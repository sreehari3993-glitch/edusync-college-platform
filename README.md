# 🎓 EduSync — Next-Gen College Digital Platform
### 🏛 TKM Institute of Technology (TKMIT), Kollam
#### *Affiliated with APJ Abdul Kalam Technological University (KTU), Kerala*

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg?style=for-the-badge&logo=openjdk)](https://adoptium.net)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen.svg?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)
[![MySQL 8.0](https://img.shields.io/badge/MySQL-8.0-blue.svg?style=for-the-badge&logo=mysql)](https://www.mysql.com/)
[![Local AI + RAG](https://img.shields.io/badge/AI%20Engine-RAG%20%2B%20NLP%20%2B%20ATS-purple.svg?style=for-the-badge&logo=openai)](https://github.com)
[![Google Drive Cloud Sync](https://img.shields.io/badge/Google%20Drive-API%20v3%20Sync-34A853.svg?style=for-the-badge&logo=googledrive)](https://developers.google.com/drive)
[![OpenPDF](https://img.shields.io/badge/PDF%20Engine-OpenPDF%201.3.40-red.svg?style=for-the-badge)](https://github.com/LibrePDF/OpenPDF)
[![ApexCharts](https://img.shields.io/badge/UI%20Charts-ApexCharts%203.x-00d4ff.svg?style=for-the-badge)](https://apexcharts.com)

---

## 🌟 Executive Overview

**EduSync** is an enterprise-grade, unified institutional governance and academic ERP platform engineered specifically for higher education engineering institutions under **APJ Abdul Kalam Technological University (KTU)**.

EduSync unifies academic compliance, multi-tier administrative governance, student lifecycle management, placement automation, and autonomous intelligence across **6 distinct institutional roles**:

```
                                    ┌────────────────────────────────────────────────────────┐
                                    │               EduSync Single-Page WebApp              │
                                    │    (Glassmorphism Dark/Light UI + ApexCharts 3.x)     │
                                    └──────────────────────────┬─────────────────────────────┘
                                                               │  Stateless REST API (JWT)
                                                               ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                            Spring Boot 3.2.3 Application Core                                             │
├───────────────────────────────┬───────────────────────────────┬──────────────────────────────┬─────────────────────────────┤
│   🤖 Campus AI & RAG Engine   │  ☁️ Google Drive Cloud Sync   │  📄 OpenPDF Certificate Hub  │  🎯 ATS Resume Screening    │
│  • Apache PDFBox & POI Parser │  • Google Drive API v3        │  • Official TKMIT Letterhead │  • 100+ Skill Taxonomy      │
│  • In-Memory TF-IDF Retrieval │  • Auto-Upload Chat Audits    │  • Cryptographic QR Tokens   │  • Weighted Matching Engine │
│  • Student Context Injection  │  • Auto-Download KTU Guides   │  • Multi-Page Export Reports │  • 1-Click Batch Screener   │
├───────────────────────────────┴───────────────────────────────┴──────────────────────────────┴─────────────────────────────┤
│                                      🏛 Multi-Tier Academic Governance & Security Layer                                    │
│   • 3-Tier Duty Leave Workflow   • Real-Time KTU Attendance & Condonation  • CIE & Internal Marks  • Spring Security (RBAC)│
└──────────────────────────────────────────────────────────────┬─────────────────────────────────────────────────────────────┘
                                                               │  Spring Data JPA / Hibernate
                                                               ▼
                                    ┌────────────────────────────────────────────────────────┐
                                    │               MySQL 8.0 Database Storage               │
                                    │   (Users, Leaves, Attendance, Marks, Drives, Audits)   │
                                    └────────────────────────────────────────────────────────┘
```

---

## 🚀 Key Modules & System Capabilities

### 1. 🤖 Campus AI Copilot & Local RAG Knowledge Engine
* **Hybrid Retrieval-Augmented Generation (RAG)**: Uses **Apache PDFBox 3.0** and **Apache POI 5.2** to extract text, tables, and rules directly from official KTU syllabus PDFs, regulations documents, timetable spreadsheets (`.xlsx`), and guidelines in `./ktu-docs/`.
* **Sub-Millisecond Heuristic Matching**: In-memory vector keyword scoring and TF-IDF relevance ranking for instant, offline answers with zero external API fees.
* **Live Student Context Injection**: Automatically enriches responses with the authenticated student's real-time attendance percentage, current active leaves, enrolled subjects, and placement drive eligibility.
* **Continuous Quality & Audit Trail**: Every student query and AI response is automatically recorded in structured JSONL (`ktu-docs/student_chat_audit_log.jsonl`) and human-readable text logs (`ktu-docs/student_chat_history.txt`).
* **Optional Local LLM Bridge**: Seamlessly connects to local **Ollama** instances (`edusync-ai` / `llama3` on `http://localhost:11434`) for conversational queries.

---

### 2. ☁️ Bi-Directional Google Drive Cloud Synchronization
* **Service Account Integration**: Powered by Google Drive API v3 and Google Auth Library (`credentials.json`).
* **Automated Cloud Backup**: Automatically uploads updated chat transcripts and audit logs to the institutional Google Drive folder.
* **Dynamic Training Ingestion**: Downloads newly added KTU circulars, PDF syllabus files, and Excel tables uploaded to Drive directly into `./ktu-docs/` and triggers automatic RAG re-indexing.
* **Scheduled Background Execution**: Runs automatically every 10 minutes via Spring `@Scheduled` background worker and supports manual trigger via `POST /api/ai/sync-drive`.

---

### 3. 🎯 AI Resume Screening & ATS Ranking Studio
* **Comprehensive Technical Taxonomy**: Built-in parser classifying 100+ technical frameworks, programming languages, database architectures, and cloud services (Java, Spring Boot, Python, React, Docker, Kubernetes, AWS, SQL, Machine Learning, etc.).
* **Multi-Factor ATS Algorithm**:
  $$\text{ATS Score} = (50\% \times \text{Skill Match}) + (30\% \times \text{CGPA Normalized}) + (20\% \times \text{Project Depth})$$
* **Placement Officer ATS Studio**: 1-click batch screening on all registered applicants for any placement drive, ranking candidates and updating scores in the database.
* **Student Resume Reviewer**: Interactive resume analyzer highlighting missing skill gaps, keyword recommendations, and actionable interview tips.

---

### 4. 📋 Multi-Tier Duty Leave Sanctioning Workflow
* **End-to-End Governance**:
  $$\text{Student Application} \longrightarrow \text{Tier 1: Faculty Advisor Review} \longrightarrow \text{Tier 2: HOD Recommendation} \longrightarrow \text{Tier 3: Principal Final Sanction}$$
* **Support for Multiple Leave Categories**: Duty Leave (NSS/Sports/Tech Fests), Medical Leave (with automated condonation calculation), and Casual Leave.
* **Automated Official Certificates**: Upon Principal approval, the system generates an official TKMIT Duty Leave Certificate with an embedded QR verification token.

---

### 5. 📊 Real-Time KTU Attendance & CIE Marks Engine
* **KTU Regulatory Compliance**:
  - **$\ge 75\%$**: Eligible for KTU End-Semester Examinations.
  - **$60\% - 74\%$**: Flagged for Principal Condonation.
  - **$< 60\%$**: Ineligible / Year-back warning.
* **Continuous Internal Evaluation (CIE)**: Dynamic aggregation of Series Exam 1, Series Exam 2, Assignments, and Attendance marks to compute final internal scores out of 50.
* **Faculty Attendance Tracker**: 1-click bulk subject attendance entry (Present / Absent / Excused) with auto-updating student trendlines.

---

### 6. 💼 Campus Placements & Company Drives
* **Drive Management**: Full lifecycle management of on-campus and off-campus recruitment drives (Eligibility CGPA, allowed branches, CTC package, deadlines).
* **Application Tracking System**: Student 1-click application submission with live status progression (`APPLIED` $\rightarrow$ `SHORTLISTED` $\rightarrow$ `INTERVIEW_SCHEDULED` $\rightarrow$ `SELECTED` $\rightarrow$ `REJECTED`).
* **Placement Funnel Analytics**: Interactive visual charts tracking department-wise offers, average package, and company distribution.

---

### 7. 🛡️ Confidential Student Grievance Redressal Cell
* **Confidential Filing**: Students can submit complaints across Academic, Hostel, Infrastructure, Placement, or Harassment categories with optional anonymous flags.
* **Administrative Escalation**: Direct routing to Department HODs and Principal with live resolution status tracking (`SUBMITTED`, `UNDER_REVIEW`, `RESOLVED`, `REJECTED`) and formal resolution memos.

---

### 8. 📄 Dynamic Document & PDF Export Engine
* Powered by **OpenPDF 1.3.40** with institutional branding, tables, metadata, and security tokens:
  - **Official Duty Leave Certificates** (Signed with dynamic QR verification token).
  - **Departmental Attendance & CIE Registers** (Official tabular printouts).
  - **Campus Placement Drives & Selection Reports** (Consolidated hiring statistics).
  - **System Security & Audit Trail Reports** (Chronological login & activity logs).

---

### 9. 🎨 Modern Glassmorphism UI & Interactive ApexCharts
* **Unified Single-Page Application (SPA)**: Ultra-fast responsive interface served directly from Spring Boot static assets.
* **Adaptive Theme Engine**: Seamless toggle between Light mode and sleek Navy Glassmorphism Dark mode (`#070d18`) with `localStorage` preference memory.
* **Interactive ApexCharts**: Real-time radial attendance meters, multi-series academic trendlines, departmental comparison bar charts, and recruitment funnel graphs.

---

## 🏛️ Institutional Role-Based Access Control (RBAC)

EduSync enforces strict role separation across **6 institutional profiles**:

| Icon | Role | Key Capabilities & Permissions |
|:---:|---|---|
| 🎓 | **Student** | Live attendance monitoring, duty leave applications, internal marks inspection, campus drive applications, AI Resume review & ATS screening, AI Copilot access, grievance filing. |
| 👨‍🏫 | **Faculty Advisor** | Subject attendance marking, CIE series marks entry, assignment management, Tier-1 advisee duty leave approvals, student academic mentorship overview. |
| 🏛 | **Head of Department (HOD)** | Departmental attendance analytics, faculty coordination, Tier-2 duty leave recommendation, grievance investigation, syllabus coverage tracking. |
| 🏛 | **Principal** | College-wide executive governance, Tier-3 final leave sanctioning, institution-wide PDF report generation, grievance resolution, security audit monitoring. |
| 💼 | **Placement Officer** | Recruitment drive creation, candidate application tracking, AI batch resume screening, placement statistics export. |
| 🛡️ | **Administrator** | System user provisioning, role assignments, system-wide broadcast notices, real-time security audit trails, database maintenance. |

---

## 📁 Repository Structure

```
edusync-backend/
├── pom.xml                                   # Maven dependencies & build configuration
├── README.md                                 # Comprehensive project documentation
├── Dockerfile                                # Multi-stage Docker container build
├── docker-compose.yml                        # Multi-service stack (MySQL 8 + Spring Boot)
├── run-backend.bat                           # Windows batch launcher
├── run-backend.ps1                           # Windows PowerShell automated launcher
│
├── ktu-docs/                                 # RAG Training Knowledge Base
│   ├── EduSync_AI_Campus_Master_Training_Guide.txt # Core KTU regulations handbook
│   ├── KTU_Academic_Regulations_2024.pdf     # KTU 2024 scheme B.Tech rules
│   ├── KTURegulation2024-ActivityPoints.pdf  # KTU 120 activity points criteria
│   ├── Placement_Drives_2024.csv             # Historical placement criteria
│   ├── courses.xlsx                          # Department course catalog
│   ├── student_chat_audit_log.jsonl          # Structured chat audit logs
│   └── student_chat_history.txt              # Human-readable chat transcripts
│
└── src/
    ├── main/
    │   ├── java/com/edusync/
    │   │   ├── EduSyncApplication.java        # Spring Boot main entry point
    │   │   │
    │   │   ├── config/                        # Infrastructure Configuration
    │   │   │   ├── GlobalExceptionHandler.java# Centralized JSON error responses
    │   │   │   └── SecurityConfig.java        # Spring Security 6 & CORS filters
    │   │   │
    │   │   ├── controller/                    # REST API Controllers (13 Controllers)
    │   │   │   ├── AiController.java          # AI Chatbot, ATS Screening & Drive Sync
    │   │   │   ├── AssignmentController.java  # Assignment management
    │   │   │   ├── AttendanceController.java  # Subject & student attendance
    │   │   │   ├── AuditLogController.java    # System audit trail logs
    │   │   │   ├── AuthController.java        # JWT Authentication & Registration
    │   │   │   ├── FrontendController.java    # SPA Frontend delivery
    │   │   │   ├── GrievanceController.java   # Student grievance redressal
    │   │   │   ├── InternalMarkController.java# CIE series marks computation
    │   │   │   ├── LeaveController.java       # 3-tier duty leave approvals & PDF
    │   │   │   ├── NoticeController.java      # Campus announcements
    │   │   │   ├── PlacementController.java   # Campus placement drives
    │   │   │   ├── ReportController.java      # Executive PDF report downloads
    │   │   │   └── UserController.java        # User profile & administration
    │   │   │
    │   │   ├── dto/                           # Data Transfer Objects
    │   │   │   ├── AiDto.java                 # Chat & Resume ATS models
    │   │   │   └── AppDto.java                # Unified API response wrappers
    │   │   │
    │   │   ├── model/                         # JPA Database Entities (12 Models)
    │   │   │   ├── Assignment.java            # Course assignment tasks
    │   │   │   ├── Attendance.java            # Daily student attendance logs
    │   │   │   ├── AuditLog.java              # Security audit trail entries
    │   │   │   ├── Grievance.java             # Student grievances & resolutions
    │   │   │   ├── InternalMark.java          # CIE series exam & attendance marks
    │   │   │   ├── LeaveRequest.java          # Multi-tier leave requests
    │   │   │   ├── Mark.java                  # Semester examination grades
    │   │   │   ├── Notice.java                # Institutional broadcast notices
    │   │   │   ├── PlacementApplication.java  # Candidate applications & AI scores
    │   │   │   ├── PlacementDrive.java        # Recruitment drive postings
    │   │   │   ├── Subject.java               # Department subject catalog
    │   │   │   └── User.java                  # User accounts & roles
    │   │   │
    │   │   ├── rag/                           # Document RAG Processing
    │   │   │   └── RagService.java            # PDFBox, POI & semantic retrieval
    │   │   │
    │   │   ├── repository/                    # Spring Data JPA Repositories
    │   │   │
    │   │   ├── security/                      # JWT Security Implementation
    │   │   │   ├── EduSyncUserDetailsService.java
    │   │   │   ├── JwtAuthenticationFilter.java
    │   │   │   └── JwtUtil.java
    │   │   │
    │   │   └── service/                       # Business Logic Layer
    │   │       ├── AiService.java             # AI NLP engine, context & ATS
    │   │       ├── AssignmentService.java     # Homework workflows
    │   │       ├── AttendanceService.java     # Attendance & condonation math
    │   │       ├── AuditService.java          # System event logging
    │   │       ├── AuthService.java           # Login verification & JWT issuance
    │   │       ├── GoogleDriveService.java    # Google Drive API v3 cloud sync
    │   │       ├── GrievanceService.java      # Redressal cell workflows
    │   │       ├── InternalMarkService.java   # CIE calculation logic
    │   │       ├── LeaveService.java          # 3-tier leave approval chains
    │   │       ├── NoticeService.java         # Campus notices
    │   │       ├── PdfService.java            # OpenPDF dynamic document creator
    │   │       ├── PlacementService.java      # Campus placements
    │   │       └── UserService.java           # User management
    │   │
    │   └── resources/
    │       ├── application.properties         # Server, DB, JWT & Drive settings
    │       ├── schema-seed.sql                # Complete DB schema & demo data
    │       └── static/
    │           └── index.html                 # Unified Responsive SPA Frontend
    │
    └── test/
        └── java/com/edusync/
            └── GoogleDriveSyncTest.java       # Automated Google Drive Sync Test
```

---

## 🛠️ Technology Stack

| Layer | Technologies |
|---|---|
| **Backend Framework** | Java 21, Spring Boot 3.2.3, Spring Data JPA, Spring Security 6 (Stateless JWT) |
| **Database & ORM** | MySQL 8.0, Hibernate 6.4 ORM, HikariCP Connection Pool |
| **AI & Document RAG** | Apache PDFBox 3.0.1, Apache POI 5.2.5, TF-IDF Vector Scorer, Local Ollama Bridge |
| **Cloud Integration** | Google Drive API v3 (`google-api-services-drive`), Google Auth Library OAuth2 |
| **Document Generation** | OpenPDF 1.3.40 (LibrePDF), ZXing QR Token Integration |
| **Frontend UI/UX** | Single-Page Application (HTML5, Vanilla CSS3 Glassmorphism, ES6+ JS, ApexCharts 3.x) |
| **DevOps & Packaging** | Maven 3.9+, Docker, Docker Compose, Windows PowerShell Launcher |

---

## 🌐 REST API Endpoints Reference

### 1. Authentication & Users (`/api/auth`, `/api/users`)
* `POST /api/auth/login` — Authenticate and receive JWT bearer token.
* `POST /api/auth/register` — Register a new student or faculty account.
* `GET /api/users/me` — Retrieve current authenticated user profile.
* `PUT /api/users/profile` — Update user profile details.
* `POST /api/users/change-password` — Change password.
* `GET /api/users` — *(Admin only)* List all institutional users.

### 2. Campus AI & Cloud Sync (`/api/ai`)
* `POST /api/ai/chat` — Campus AI query with live student context injection & RAG search.
* `POST /api/ai/screen-resumes` — Batch ATS resume screening for recruitment drives.
* `POST /api/ai/analyze-resume` — Student resume review with skill gap suggestions.
* `GET /api/ai/screen-drive/{driveId}` — Run ATS screening on all applicants of a drive.
* `GET /api/ai/drive-status` — Check Google Drive cloud sync status and folder URL.
* `POST /api/ai/sync-drive` — Manually trigger bi-directional Google Drive synchronization.

### 3. Attendance & CIE Marks (`/api/attendance`, `/api/internal-marks`)
* `GET /api/attendance/my-summary` — Student attendance summary and KTU condonation alert.
* `GET /api/attendance/subject/{subjectId}` — Subject-wise attendance breakdown.
* `POST /api/attendance/mark` — Faculty batch attendance entry.
* `GET /api/internal-marks/my-marks` — Student CIE series exam and internal marks.
* `POST /api/internal-marks/save` — Faculty CIE marks entry and automatic total calculation.

### 4. Duty Leaves & Certificates (`/api/leaves`)
* `POST /api/leaves/apply` — Submit a new Duty / Medical / Casual leave application.
* `GET /api/leaves/my-leaves` — View student submitted leave history and status.
* `GET /api/leaves/pending` — View pending leaves awaiting current role's approval.
* `POST /api/leaves/{id}/approve` — Approve leave (Tier 1: Advisor, Tier 2: HOD, Tier 3: Principal).
* `POST /api/leaves/{id}/reject` — Reject leave with required rejection remarks.
* `GET /api/leaves/{id}/certificate` — Download official OpenPDF Duty Leave Certificate.

### 5. Placements & Drives (`/api/placements`)
* `GET /api/placements/drives` — List active campus placement recruitment drives.
* `POST /api/placements/drives` — Placement Officer creates a new recruitment drive.
* `POST /api/placements/apply/{driveId}` — Student 1-click application for an eligible drive.
* `GET /api/placements/applications` — View student submitted placement applications.
* `GET /api/placements/analytics` — Consolidated placement analytics and hiring funnels.

### 6. Grievances & Redressal (`/api/grievances`)
* `POST /api/grievances/submit` — Submit a grievance (Academic, Harassment, Hostel, etc.).
* `GET /api/grievances/my-grievances` — View status of student filed grievances.
* `GET /api/grievances/all` — *(HOD / Principal / Admin)* View all institutional grievances.
* `POST /api/grievances/{id}/resolve` — Update grievance status and attach resolution remarks.

### 7. Executive PDF Reports (`/api/reports`)
* `GET /api/reports/leaves/pdf` — Export Institutional Leave Sanction Registry PDF.
* `GET /api/reports/placements/pdf` — Export Campus Placement Hiring Statistics PDF.
* `GET /api/reports/audit-logs/pdf` — Export Security Audit & Access Trail PDF.

---

## ⚡ Quickstart & Installation

### Prerequisites
* **Java Development Kit (JDK) 17 or 21** (`JAVA_HOME` configured)
* **Apache Maven 3.8+**
* **MySQL Server 8.0** running on port `3306`

---

### Option A: Local Development Setup

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/sreehari3993-glitch/edusync-college-platform.git
   cd edusync-college-platform
   ```

2. **Initialize the Database:**
   ```sql
   CREATE DATABASE edusync_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
   *The database tables and seed accounts are automatically created and updated on application startup.*

3. **Configure Database Credentials:**
   Edit `src/main/resources/application.properties` with your local MySQL credentials:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/edusync_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true
   spring.datasource.username=root
   spring.datasource.password=YOUR_PASSWORD
   ```

4. **Launch the Application:**
   ```powershell
   # On Windows (PowerShell)
   .\run-backend.ps1

   # Or using standard Maven
   mvn spring-boot:run
   ```

5. **Open in Browser:**
   👉 Navigate to **`http://localhost:8081`**

---

### Option B: Docker Compose Setup

Run the entire application along with a dedicated MySQL 8.0 container in one command:

```bash
docker-compose up --build -d
```

* Backend: `http://localhost:8081`
* MySQL Port: `localhost:3307`

---

## 🔑 Pre-Seeded Demo Accounts

All pre-seeded demo accounts are ready for testing with default credentials:

| Role | Name | Email | Password |
|---|---|---|---|
| 🎓 **Student (CSE S6)** | Anzal Rahman | `anzal.r@student.tkmit.ac.in` | `student123` |
| 🎓 **Student (CSE S6)** | Devika Nair | `devika.n@student.tkmit.ac.in` | `student123` |
| 👨‍🏫 **Faculty Advisor** | Prof. Arun Kumar | `prof.arun@faculty.tkmit.ac.in` | `faculty123` |
| 🏛 **HOD (CSE Dept)** | Dr. Rajesh V | `hod.cse@tkmit.ac.in` | `hod123` |
| 🏛 **Principal** | Dr. S. Radhakrishnan | `principal@tkmit.ac.in` | `principal123` |
| 💼 **Placement Officer**| Prof. Meera Thomas | `placement@tkmit.ac.in` | `placement123` |
| 🛡️ **System Administrator** | Institutional Admin | `admin@tkmit.ac.in` | `admin123` |

---

## 🧪 Testing & Verification

Run the full automated test suite, including the Google Drive cloud sync integration:

```bash
mvn test
```

To run only the Google Drive synchronization verification:
```bash
mvn test -Dtest=GoogleDriveSyncTest
```

---

## 📜 Academic Attribution & License

Developed for academic presentation, institutional governance, and technical demonstration at:
**TKM Institute of Technology (TKMIT), Karuvelil, Kollam, Kerala — 691505**
Affiliated with **APJ Abdul Kalam Technological University (KTU)**.

*Licensed under the [MIT License](LICENSE).*
