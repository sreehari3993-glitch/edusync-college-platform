# 🎓 EduSync — Next-Gen College Digital Platform
### 🏛 TKM Institute of Technology (TKMIT), Kerala

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg?style=flat&logo=openjdk)](https://adoptium.net)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen.svg?style=flat&logo=springboot)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg?style=flat&logo=mysql)](https://www.mysql.com/)
[![Local AI](https://img.shields.io/badge/AI%20Engine-Local%20NLP%20%2B%20ATS-purple.svg?style=flat)](https://github.com)
[![ApexCharts](https://img.shields.io/badge/Charts-ApexCharts%20Interactive-00d4ff.svg?style=flat)](https://apexcharts.com)
[![PDF](https://img.shields.io/badge/PDF-OpenPDF%201.3.40-red.svg?style=flat)](https://github.com/LibrePDF/OpenPDF)

---

## 🌟 Overview

**EduSync** is a unified, full-stack institutional ERP and academic governance platform designed specifically for engineering colleges affiliated with **APJ Abdul Kalam Technological University (KTU)**.

It centralizes multi-tier governance across **6 distinct institutional roles**, automating end-to-end duty leave sanctioning, real-time KTU attendance & condonation monitoring, Continuous Internal Evaluation (CIE) calculation, campus placement drives, and automated **AI-powered Resume ATS Screening & Campus Chatbot**.

---

## ✨ Key Features & Innovations

### 1. 🤖 Local AI Campus Copilot & Chatbot
- **100% Local & Offline**: Deterministic campus NLP knowledge engine with sub-millisecond latency and zero API cost.
- **Deep KTU Regulatory Intelligence**: Explains 75% attendance policy, 65%–74% condonation rules, medical leave regularisation, and internal marks formulas.
- **Personalized Context Awareness**: Live database context retrieval for authenticated students to report personal attendance %, active leave statuses, and drive eligibility.
- **Global Floating Copilot Widget**: Accessible on any page via the `✨ Ask EduAI` floating trigger.

### 2. 🎯 AI Resume Screening & ATS Ranking Engine
- **Automated Skills Parsing**: Built-in taxonomy covering 100+ technical frameworks, programming languages, and engineering tools.
- **Weighted ATS Algorithm**: Computes holistic match scores (50% Skills, 30% CGPA, 20% Projects/Experience).
- **Recruiter Studio**: Placement officers can run 1-click batch screening on candidate pools to filter and shortlist applicants.
- **Student ATS Optimizer**: Interactive resume reviewer that identifies skill gaps and gives tailored improvement suggestions.

### 3. 📄 Dynamic Digital PDF Generator
- **Official TKMIT Letterhead & Formatting**: Powered by OpenPDF with institutional headers, metadata tables, and official signatory blocks.
- **Official Duty Leave Certificates**: Auto-generated upon final Principal/HOD approval with QR verification tokens.
- **Executive Institutional Reports**: 1-click PDF exports for Leave Registries, Placement Statistics, and Security Audit Logs.

### 4. 🌙 Sleek Glassmorphism Dark Mode & ApexCharts
- **Adaptive Theme Engine**: Smooth toggle between Light and deep navy Dark mode (`#070d18`) with `localStorage` persistence.
- **Interactive Role Dashboards**: Responsive ApexCharts radial bars, performance curve spline areas, departmental attendance trendlines, and placement hiring funnels.

---

## 🏛 Institutional Role Architecture

| Role | Key Capabilities |
|---|---|
| **🎓 Student** | View live attendance %, track multi-stage duty leave requests, file confidential grievances, take AI resume reviews, download approved certificates. |
| **👨‍🏫 Faculty Advisor** | Review advisee leave requests, mark daily subject attendance, enter Series/Assignment marks, upload study notes and assignments. |
| **🏛 Head of Department (HOD)** | Departmental attendance trend analysis, faculty overview, HOD-tier leave sanctions, grievance resolution. |
| **🏛 Principal** | College-wide executive governance, final duty leave approvals, institutional audit logs, and performance oversight. |
| **💼 Placement Officer** | Manage company recruitment drives, run AI resume screening, track student application funnels, export reports. |
| **🛡️ Administrator** | System user management, role access controls, security audit log monitoring, and institutional notice broadcasts. |

---

## 📁 Project File Structure

```
edusync-backend/
├── pom.xml                               # Maven project dependencies & build configuration
├── README.md                             # Comprehensive project documentation
├── run-backend.bat                       # Windows one-click batch launcher
├── run-backend.ps1                       # PowerShell automated runner
│
└── src/main/
    ├── java/com/edusync/
    │   ├── EduSyncApplication.java        # Spring Boot application entry point
    │   │
    │   ├── config/                       # Configuration Layer
    │   │   ├── GlobalExceptionHandler.java # Centralized JSON error handling
    │   │   └── SecurityConfig.java       # JWT Security & CORS filter chains
    │   │
    │   ├── controller/                   # REST API Controllers
    │   │   ├── AiController.java         # AI Chatbot & Resume Screening endpoints
    │   │   ├── AssignmentController.java # Course assignment endpoints
    │   │   ├── AttendanceController.java # Student & subject attendance endpoints
    │   │   ├── AuditLogController.java   # Security audit trail endpoints
    │   │   ├── AuthController.java       # Login & registration endpoints
    │   │   ├── FrontendController.java   # Single-Page Application delivery
    │   │   ├── GrievanceController.java  # Student grievance redressal endpoints
    │   │   ├── InternalMarkController.java# CIE series marks endpoints
    │   │   ├── LeaveController.java      # Multi-tier leave & PDF endpoints
    │   │   ├── NoticeController.java     # Campus announcement endpoints
    │   │   ├── PlacementController.java  # Campus placement drive endpoints
    │   │   ├── ReportController.java     # PDF institutional export endpoints
    │   │   └── UserController.java       # Profile, password, & admin endpoints
    │   │
    │   ├── dto/                          # Data Transfer Objects
    │   │   ├── AiDto.java                # Chat & Resume ATS request/response models
    │   │   └── AppDto.java               # Unified application DTOs & ApiResponse wrapper
    │   │
    │   ├── model/                        # JPA Database Entities
    │   │   ├── Assignment.java           # Homework & course tasks
    │   │   ├── Attendance.java           # Daily attendance logs
    │   │   ├── AuditLog.java             # System security trails
    │   │   ├── Grievance.java            # Student feedback & complaints
    │   │   ├── InternalMark.java         # Series exam & assignment marks
    │   │   ├── LeaveRequest.java         # Duty, medical, & casual leaves
    │   │   ├── Mark.java                 # Semester exam marks
    │   │   ├── Notice.java               # Campus announcements
    │   │   ├── PlacementApplication.java # Student job applications & AI scores
    │   │   ├── PlacementDrive.java       # Company recruitment drives
    │   │   ├── Subject.java              # Academic course definitions
    │   │   ├── Timetable.java            # Weekly class schedules
    │   │   └── User.java                 # System users & institutional roles
    │   │
    │   ├── repository/                   # Spring Data JPA Repositories
    │   ├── security/                     # JWT Authentication & Authorization
    │   │   ├── EduSyncUserDetailsService.java
    │   │   ├── JwtAuthenticationFilter.java
    │   │   └── JwtUtil.java
    │   │
    │   └── service/                      # Business Logic Layer
    │       ├── AiService.java            # Local AI Engine (Chatbot + ATS Resume Screener)
    │       ├── AssignmentService.java    # Assignment workflows
    │       ├── AttendanceService.java    # Attendance calculation & condonation checks
    │       ├── AuditService.java         # Security logging service
    │       ├── AuthService.java          # Authentication & token generation
    │       ├── GrievanceService.java     # Grievance lifecycle management
    │       ├── InternalMarkService.java  # CIE calculation logic
    │       ├── LeaveService.java         # Leave approval chains & PDF triggers
    │       ├── NoticeService.java        # Broadcast notices service
    │       ├── PdfService.java           # OpenPDF document generator
    │       ├── PlacementService.java     # Campus recruitment management
    │       └── UserService.java          # User provisioning & profile management
    │
    └── resources/
        ├── application.properties        # MySQL connection & server configuration
        ├── schema-seed.sql               # Database schema & initial seed data
        └── static/
            └── index.html                # Unified Responsive Single-Page Application (SPA)
```

---

## 🛠️ Tech Stack

- **Backend**: Java 21, Spring Boot 3.2.3, Spring Data JPA, Spring Security (Stateless JWT)
- **Database**: MySQL 8.0, Hibernate 6.4 ORM, HikariCP Connection Pooling
- **PDF Engine**: OpenPDF 1.3.40 (iText fork)
- **Local AI Engine**: Built-in Natural Language Parser + Skill Taxonomy Matcher + Optional Ollama Local LLM Bridge
- **Frontend**: Vanilla HTML5, Modern CSS3 Glassmorphism tokens, ES6+ JavaScript, ApexCharts 3.x

---

## 🚀 Quickstart & Installation

### 1. Prerequisites
- **JDK 17 or 21** installed (`JAVA_HOME` configured)
- **Apache Maven 3.8+**
- **MySQL Server 8.0** running on port `3306`

### 2. Database Initialization
1. In MySQL, create the database:
   ```sql
   CREATE DATABASE edusync_db;
   ```
2. Execute the seed script located at `src/main/resources/schema-seed.sql`.

### 3. Run the Application
You can run the application using any of the following methods:

**Method A: One-Click Script (Windows)**
```powershell
.\run-backend.ps1
```

**Method B: Maven Command Line**
```bash
mvn clean package -DskipTests
java -jar target/edusync-backend-1.0.0.jar
```

**Method C: Spring Boot Plugin**
```bash
mvn spring-boot:run
```

4. Open your browser and navigate to:
   👉 **`http://localhost:8081`**

---

## 🔑 Default Test Credentials

All pre-seeded demo accounts use the standard password format:

| Role | Email | Password |
|---|---|---|
| **🎓 Student** | `anzal.r@student.tkmit.ac.in` | `student123` |
| **🎓 Student 2** | `devika.n@student.tkmit.ac.in` | `student123` |
| **👨‍🏫 Faculty Advisor** | `prof.arun@faculty.tkmit.ac.in` | `faculty123` |
| **🏛 HOD (CSE)** | `hod.cse@tkmit.ac.in` | `hod123` |
| **🏛 Principal** | `principal@tkmit.ac.in` | `principal123` |
| **💼 Placement Officer** | `placement@tkmit.ac.in` | `placement123` |
| **🛡️ Administrator** | `admin@tkmit.ac.in` | `admin123` |

---

## 📄 License
Developed for academic presentation and institutional showcase at **TKM Institute of Technology, Kollam**.
