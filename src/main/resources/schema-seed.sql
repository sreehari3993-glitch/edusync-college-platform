-- ═══════════════════════════════════════════════════════════════════
--  EduSync — MySQL Database Schema
--  TKM Institute of Technology, Kollam, Kerala
--
--  Run this in MySQL Workbench:
--    File → Open SQL Script → select this file → Execute (⚡)
--
--  Connection: root / sreehari_99  |  DB: edusync_db
-- ═══════════════════════════════════════════════════════════════════

CREATE DATABASE IF NOT EXISTS edusync_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE edusync_db;

-- ─── Drop tables (clean re-run) ──────────────────────────────────────────────
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS industry_activity_logs;
DROP TABLE IF EXISTS alumni_contacts;
DROP TABLE IF EXISTS advisory_members;
DROP TABLE IF EXISTS industry_projects;
DROP TABLE IF EXISTS guest_lectures;
DROP TABLE IF EXISTS industry_visits;
DROP TABLE IF EXISTS internship_applications;
DROP TABLE IF EXISTS internships;
DROP TABLE IF EXISTS industry_mous;
DROP TABLE IF EXISTS companies;
DROP TABLE IF EXISTS study_materials;
DROP TABLE IF EXISTS internal_marks;
DROP TABLE IF EXISTS grievances;
DROP TABLE IF EXISTS device_tokens;
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS assignment_submissions;
DROP TABLE IF EXISTS advisor_requests;
DROP TABLE IF EXISTS placement_applications;
DROP TABLE IF EXISTS placement_drives;
DROP TABLE IF EXISTS marks;
DROP TABLE IF EXISTS attendance;
DROP TABLE IF EXISTS timetable;
DROP TABLE IF EXISTS leave_requests;
DROP TABLE IF EXISTS assignments;
DROP TABLE IF EXISTS notices;
DROP TABLE IF EXISTS subjects;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

-- ─── USERS ───────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(120)  NOT NULL,
    email          VARCHAR(150)  NOT NULL UNIQUE,
    password       VARCHAR(255)  NOT NULL,  -- bcrypt hashed
    role           ENUM('STUDENT','FACULTY','HOD','PRINCIPAL','PLACEMENT_OFFICER','ADMIN','INDUSTRIAL_CONNECT_HEAD') NOT NULL,
    roll_number    VARCHAR(30),
    employee_id    VARCHAR(30),
    department     VARCHAR(50),
    semester       VARCHAR(10),
    section        VARCHAR(5),
    phone          VARCHAR(20),
    parent_email   VARCHAR(150),
    parent_phone   VARCHAR(50),
    advisor_id     BIGINT,
    profile_pic    VARCHAR(255),
    is_active      TINYINT(1)   DEFAULT 1,
    created_at     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (advisor_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_email (email),
    INDEX idx_role  (role),
    INDEX idx_dept  (department)
) ENGINE=InnoDB;

-- ─── SUBJECTS ────────────────────────────────────────────────────────────────
CREATE TABLE subjects (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    code        VARCHAR(20)  NOT NULL,
    department  VARCHAR(50),
    semester    VARCHAR(10),
    credits     INT,
    faculty_id  BIGINT,
    is_active   TINYINT(1) DEFAULT 1,
    FOREIGN KEY (faculty_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_dept_sem (department, semester)
) ENGINE=InnoDB;

-- ─── LEAVE REQUESTS ──────────────────────────────────────────────────────────
CREATE TABLE leave_requests (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id           BIGINT NOT NULL,
    leave_type           ENUM('DUTY_LEAVE','MEDICAL_LEAVE','OD_LETTER','EMERGENCY_LEAVE','CASUAL_LEAVE') NOT NULL,
    reason               TEXT NOT NULL,
    from_date            DATE NOT NULL,
    to_date              DATE NOT NULL,
    no_of_days           INT DEFAULT 1,
    event_name           VARCHAR(200),
    attachment_path      VARCHAR(255),
    overall_status       ENUM('PENDING','APPROVED','REJECTED','NOT_REQUIRED') DEFAULT 'PENDING',
    faculty_status       ENUM('PENDING','APPROVED','REJECTED','NOT_REQUIRED') DEFAULT 'PENDING',
    faculty_remark       VARCHAR(500),
    faculty_approved_by  VARCHAR(120),
    faculty_approved_at  DATETIME,
    hod_status           ENUM('PENDING','APPROVED','REJECTED','NOT_REQUIRED') DEFAULT 'PENDING',
    hod_remark           VARCHAR(500),
    hod_approved_by      VARCHAR(120),
    hod_approved_at      DATETIME,
    principal_status     ENUM('PENDING','APPROVED','REJECTED','NOT_REQUIRED') DEFAULT 'NOT_REQUIRED',
    principal_remark     VARCHAR(500),
    principal_approved_at DATETIME,
    current_stage        VARCHAR(50) DEFAULT 'FACULTY_REVIEW',
    pdf_path             VARCHAR(255),
    created_at           DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_student (student_id),
    INDEX idx_overall_status (overall_status)
) ENGINE=InnoDB;

-- ─── NOTICES ─────────────────────────────────────────────────────────────────
CREATE TABLE notices (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(250) NOT NULL,
    content     TEXT NOT NULL,
    category    ENUM('ACADEMIC','ADMINISTRATIVE','PLACEMENT','EVENTS','URGENT') DEFAULT 'ACADEMIC',
    visibility  ENUM('ENTIRE_COLLEGE','STUDENTS_ONLY','FACULTY_ONLY','DEPARTMENT_ONLY') DEFAULT 'ENTIRE_COLLEGE',
    created_by  BIGINT,
    expires_at  DATETIME,
    is_active   TINYINT(1) DEFAULT 1,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ─── ATTENDANCE ───────────────────────────────────────────────────────────────
CREATE TABLE assignments (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject     VARCHAR(120) NOT NULL,
    title       VARCHAR(250) NOT NULL,
    description TEXT NOT NULL,
    department  VARCHAR(50) NOT NULL,
    semester    VARCHAR(10),
    max_marks   INT DEFAULT 20,
    due_date    DATE NOT NULL,
    created_by  BIGINT,
    is_active   TINYINT(1) DEFAULT 1,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_assignment_target (department, semester),
    INDEX idx_assignment_due (due_date)
) ENGINE=InnoDB;

CREATE TABLE attendance (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id    BIGINT NOT NULL,
    subject_id    BIGINT NOT NULL,
    faculty_id    BIGINT,
    date          DATE NOT NULL,
    status        ENUM('PRESENT','ABSENT','OD','HOLIDAY','MEDICAL_LEAVE') DEFAULT 'PRESENT',
    period_number INT,
    remarks       VARCHAR(255),
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_student_subject_date (student_id, subject_id, date),
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    FOREIGN KEY (faculty_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_student_subject (student_id, subject_id)
) ENGINE=InnoDB;

-- ─── MARKS ───────────────────────────────────────────────────────────────────
CREATE TABLE marks (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id       BIGINT NOT NULL,
    subject_id       BIGINT NOT NULL,
    exam_type        ENUM('INTERNAL_1','INTERNAL_2','INTERNAL_3','MODEL_EXAM','UNIVERSITY_EXAM','ASSIGNMENT','LAB') NOT NULL,
    marks_obtained   DECIMAL(6,2),
    max_marks        DECIMAL(6,2),
    grade            VARCHAR(5),
    semester         VARCHAR(10),
    academic_year    VARCHAR(15),
    entered_by       VARCHAR(120),
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
    INDEX idx_student_sem (student_id, semester)
) ENGINE=InnoDB;

-- ─── PLACEMENT DRIVES ────────────────────────────────────────────────────────
CREATE TABLE placement_drives (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_name         VARCHAR(150) NOT NULL,
    logo                 VARCHAR(10),
    role                 VARCHAR(150) NOT NULL,
    description          TEXT,
    package_lpa          DECIMAL(5,2),
    drive_date           DATE,
    last_date_to_apply   DATE,
    min_cgpa             DECIMAL(3,1),
    eligible_branches    VARCHAR(100),
    eligible_batch       VARCHAR(10),
    job_type             VARCHAR(30),
    location             VARCHAR(100),
    status               ENUM('UPCOMING','ONGOING','COMPLETED','CANCELLED') DEFAULT 'UPCOMING',
    created_at           DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ─── PLACEMENT APPLICATIONS ───────────────────────────────────────────────────
CREATE TABLE placement_applications (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id  BIGINT NOT NULL,
    drive_id    BIGINT NOT NULL,
    resume_path VARCHAR(255),
    cgpa        DECIMAL(3,1),
    status      ENUM('APPLIED','SHORTLISTED','SELECTED','REJECTED') DEFAULT 'APPLIED',
    ai_score    INT,
    applied_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_student_drive (student_id, drive_id),
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (drive_id)   REFERENCES placement_drives(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ─── TIMETABLE ───────────────────────────────────────────────────────────────
CREATE TABLE timetable (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    department   VARCHAR(50) NOT NULL,
    semester     VARCHAR(10) NOT NULL,
    section      VARCHAR(5),
    day_of_week  ENUM('MON','TUE','WED','THU','FRI') NOT NULL,
    period_number INT NOT NULL,
    start_time   VARCHAR(10),
    end_time     VARCHAR(10),
    subject_id   BIGINT,
    faculty_id   BIGINT,
    room         VARCHAR(50),
    is_break     TINYINT(1) DEFAULT 0,
    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE SET NULL,
    FOREIGN KEY (faculty_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ─── ADVISOR REQUESTS ─────────────────────────────────────────────────────────
CREATE TABLE advisor_requests (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id      BIGINT NOT NULL,
    faculty_id      BIGINT NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    message         VARCHAR(500),
    response_remark VARCHAR(500),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    responded_at    DATETIME,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (faculty_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ─── ASSIGNMENT SUBMISSIONS ───────────────────────────────────────────────────
CREATE TABLE assignment_submissions (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id  BIGINT NOT NULL,
    student_id     BIGINT NOT NULL,
    file_name      VARCHAR(255) NOT NULL,
    file_url       TEXT,
    download_url   TEXT,
    drive_file_id  VARCHAR(255),
    file_size      BIGINT,
    marks_obtained DOUBLE,
    feedback       TEXT,
    status         VARCHAR(50) DEFAULT 'SUBMITTED',
    submitted_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ─── AUDIT LOGS ───────────────────────────────────────────────────────────────
CREATE TABLE audit_logs (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    action        VARCHAR(255) NOT NULL,
    actor_email   VARCHAR(150) NOT NULL,
    actor_name    VARCHAR(150),
    actor_role    VARCHAR(50),
    target_entity VARCHAR(100),
    target_id     VARCHAR(100),
    details       TEXT,
    ip_address    VARCHAR(100),
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ─── DEVICE TOKENS (Push Notifications) ───────────────────────────────────────
CREATE TABLE device_tokens (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    token        VARCHAR(512) NOT NULL,
    platform     VARCHAR(50),
    device_model VARCHAR(100),
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_user_token (user_id, token),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ─── GRIEVANCES ──────────────────────────────────────────────────────────────
CREATE TABLE grievances (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id   BIGINT NOT NULL,
    category     VARCHAR(50) NOT NULL,
    subject      VARCHAR(255) NOT NULL,
    description  TEXT NOT NULL,
    target_role  VARCHAR(50) NOT NULL,
    department   VARCHAR(50),
    status       VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    response     TEXT,
    responded_by VARCHAR(150),
    responded_at DATETIME,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ─── INTERNAL MARKS ──────────────────────────────────────────────────────────
CREATE TABLE internal_marks (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id       BIGINT NOT NULL,
    subject_name     VARCHAR(150) NOT NULL,
    series_name      VARCHAR(100) NOT NULL,
    test_marks       DOUBLE DEFAULT 0.0,
    assignment_marks DOUBLE DEFAULT 0.0,
    practical_marks  DOUBLE DEFAULT 0.0,
    total_marks      DOUBLE DEFAULT 0.0,
    max_marks        DOUBLE DEFAULT 50.0,
    grade            VARCHAR(10),
    semester         VARCHAR(10),
    department       VARCHAR(50),
    entered_by       VARCHAR(150),
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ─── STUDY MATERIALS ─────────────────────────────────────────────────────────
CREATE TABLE study_materials (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    title          VARCHAR(255) NOT NULL,
    subject        VARCHAR(150) NOT NULL,
    module         VARCHAR(100),
    department     VARCHAR(50) NOT NULL,
    semester       VARCHAR(10),
    file_name      VARCHAR(255) NOT NULL,
    file_type      VARCHAR(50),
    file_size      BIGINT,
    file_url       TEXT,
    download_url   TEXT,
    drive_file_id  VARCHAR(255),
    uploaded_by    BIGINT,
    download_count INT DEFAULT 0,
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ─── COMPANIES (Shared Partner Registry) ─────────────────────────────────────
CREATE TABLE companies (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(255) NOT NULL UNIQUE,
    logo              VARCHAR(255) NULL,
    location          VARCHAR(255) NULL,
    description       TEXT NULL,
    sector            VARCHAR(150) NULL,
    website           VARCHAR(255) NULL,
    hr_name           VARCHAR(150) NULL,
    hr_email          VARCHAR(150) NULL,
    hr_phone          VARCHAR(50) NULL,
    partnership_stage VARCHAR(50) NOT NULL DEFAULT 'LEAD',
    tier              VARCHAR(50) NULL,
    placement_ready   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_companies_sector (sector),
    INDEX idx_companies_stage (partnership_stage)
) ENGINE=InnoDB;

-- ─── INDUSTRY MOUS (Institutional Agreements) ────────────────────────────────
CREATE TABLE industry_mous (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id    BIGINT NOT NULL,
    title         VARCHAR(255) NOT NULL,
    signed_date   DATE NULL,
    valid_from    DATE NULL,
    valid_to      DATE NULL,
    scope         TEXT NULL,
    status        VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    drive_file_id VARCHAR(255) NULL,
    departments   VARCHAR(255) NULL,
    document_url  VARCHAR(500) NULL,
    remarks       TEXT NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE RESTRICT,
    INDEX idx_mou_status (status),
    INDEX idx_mou_valid_to (valid_to)
) ENGINE=InnoDB;

-- ─── INTERNSHIPS (Industry Sourced Internships) ──────────────────────────────
CREATE TABLE internships (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id      BIGINT NOT NULL,
    title           VARCHAR(255) NOT NULL,
    description     TEXT NULL,
    required_skills TEXT NULL,
    duration        VARCHAR(100) NULL,
    stipend         VARCHAR(100) NULL,
    mode            VARCHAR(50) NULL,
    departments     VARCHAR(255) NULL,
    seats           INT NULL,
    deadline        DATE NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    location        VARCHAR(255) NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE RESTRICT,
    INDEX idx_internship_status (status),
    INDEX idx_internship_deadline (deadline)
) ENGINE=InnoDB;

-- ─── INTERNSHIP APPLICATIONS ─────────────────────────────────────────────────
CREATE TABLE internship_applications (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    internship_id   BIGINT NOT NULL,
    student_id      BIGINT NOT NULL,
    resume_url      VARCHAR(500) NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'APPLIED',
    certificate_url VARCHAR(500) NULL,
    feedback        TEXT NULL,
    applied_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (internship_id) REFERENCES internships(id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_ia_student_internship (internship_id, student_id),
    INDEX idx_ia_status (status)
) ENGINE=InnoDB;

-- ─── INDUSTRY VISITS (Multi-Stage Approval Pipeline) ─────────────────────────
CREATE TABLE industry_visits (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id            BIGINT NOT NULL,
    proposed_by_id        BIGINT NOT NULL,
    department            VARCHAR(100) NOT NULL,
    date                  DATE NOT NULL,
    student_count         INT NULL,
    purpose               TEXT NULL,
    location              VARCHAR(255) NULL,
    overnight             BOOLEAN NOT NULL DEFAULT FALSE,
    outside_kerala        BOOLEAN NOT NULL DEFAULT FALSE,
    stage                 VARCHAR(50) NOT NULL DEFAULT 'ICH_REVIEW',
    status                VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    ich_status            VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    ich_remark            VARCHAR(500) NULL,
    ich_approved_by       VARCHAR(150) NULL,
    ich_approved_at       DATETIME NULL,
    hod_status            VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    hod_remark            VARCHAR(500) NULL,
    hod_approved_by       VARCHAR(150) NULL,
    hod_approved_at       DATETIME NULL,
    principal_status      VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    principal_remark      VARCHAR(500) NULL,
    principal_approved_at DATETIME NULL,
    pdf_path              VARCHAR(500) NULL,
    report_drive_file_id  VARCHAR(255) NULL,
    created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE RESTRICT,
    FOREIGN KEY (proposed_by_id) REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_visit_stage (stage),
    INDEX idx_visit_status (status),
    INDEX idx_visit_dept (department)
) ENGINE=InnoDB;

-- ─── GUEST LECTURES & EXPERT SESSIONS ────────────────────────────────────────
CREATE TABLE guest_lectures (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    speaker        VARCHAR(150) NOT NULL,
    designation    VARCHAR(150) NULL,
    company_id     BIGINT NULL,
    company_name   VARCHAR(255) NULL,
    topic          VARCHAR(255) NOT NULL,
    date           DATE NOT NULL,
    time           VARCHAR(50) NULL,
    venue          VARCHAR(255) NULL,
    departments    VARCHAR(255) NULL,
    attendance     INT NULL,
    feedback_avg   DOUBLE NULL,
    coordinator_id BIGINT NULL,
    status         VARCHAR(50) NOT NULL DEFAULT 'UPCOMING',
    drive_file_id  VARCHAR(255) NULL,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE SET NULL,
    FOREIGN KEY (coordinator_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_lecture_status (status),
    INDEX idx_lecture_date (date)
) ENGINE=InnoDB;

-- ─── INDUSTRY PROJECTS (Capstone & Sponsored Consultancy) ────────────────────
CREATE TABLE industry_projects (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id        BIGINT NOT NULL,
    title             VARCHAR(255) NOT NULL,
    problem_statement TEXT NOT NULL,
    department        VARCHAR(100) NULL,
    mentor            VARCHAR(150) NULL,
    team_students     TEXT NULL,
    status            VARCHAR(50) NOT NULL DEFAULT 'PROPOSED',
    outcome           TEXT NULL,
    academic_year     VARCHAR(50) NULL,
    drive_file_id     VARCHAR(255) NULL,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE RESTRICT,
    INDEX idx_project_status (status),
    INDEX idx_project_dept (department)
) ENGINE=InnoDB;

-- ─── ADVISORY BOARD MEMBERS ──────────────────────────────────────────────────
CREATE TABLE advisory_members (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(150) NOT NULL,
    designation  VARCHAR(150) NULL,
    company_id   BIGINT NULL,
    company_name VARCHAR(255) NULL,
    email        VARCHAR(150) NULL,
    phone        VARCHAR(50) NULL,
    domain       VARCHAR(150) NULL,
    bio          TEXT NULL,
    tenure_start DATE NULL,
    tenure_end   DATE NULL,
    status       VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE SET NULL,
    INDEX idx_advisory_status (status)
) ENGINE=InnoDB;

-- ─── CORPORATE ALUMNI CONTACTS ───────────────────────────────────────────────
CREATE TABLE alumni_contacts (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(150) NOT NULL,
    batch             VARCHAR(50) NULL,
    degree            VARCHAR(100) NULL,
    department        VARCHAR(100) NULL,
    current_company   VARCHAR(255) NULL,
    current_role      VARCHAR(150) NULL,
    email             VARCHAR(150) NULL,
    phone             VARCHAR(50) NULL,
    linkedin_url      VARCHAR(500) NULL,
    willing_to_mentor BOOLEAN NOT NULL DEFAULT TRUE,
    willing_to_refer  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_alumni_company (current_company),
    INDEX idx_alumni_dept (department)
) ENGINE=InnoDB;

-- ─── INDUSTRY ACTIVITY LOGS (Audit Trail) ────────────────────────────────────
CREATE TABLE industry_activity_logs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor       VARCHAR(150) NOT NULL,
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id   BIGINT NULL,
    details     TEXT NULL,
    timestamp   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_activity_entity (entity_type, entity_id),
    INDEX idx_activity_time (timestamp)
) ENGINE=InnoDB;


-- ═══════════════════════════════════════════════════════════════════
--  SEED DATA — TKMIT Sample Data
--
--  Passwords are bcrypt of:
--    admin123       → ADMIN
--    student123     → all students
--    faculty123     → all faculty/staff
--    principal123   → principal
-- ═══════════════════════════════════════════════════════════════════

-- ─── ADMIN ───────────────────────────────────────────────────────────────────
INSERT INTO users (name, email, password, role, employee_id, department) VALUES
('System Admin', 'admin@tkmit.ac.in',
 '$2a$10$bEeadXNz7ocOJNeK4RF7gefYy2ITUaLWmF1MUtAdcLV/EgHeZT6Tq',
 'ADMIN', 'ADM001', 'Administration');

-- ─── PRINCIPAL ───────────────────────────────────────────────────────────────
INSERT INTO users (name, email, password, role, employee_id) VALUES
('Dr. K. Radhakrishnan', 'principal@tkmit.ac.in',
 '$2a$10$OSFHKNe55GOj7Dz1KM7MvOES/cPCJHPeM4e52BRoP/vbRVjmAwdN2',
 'PRINCIPAL', 'PRIN001');

-- ─── HODs ────────────────────────────────────────────────────────────────────
INSERT INTO users (name, email, password, role, employee_id, department) VALUES
('Dr. Anitha Krishnan',   'hod.cse@tkmit.ac.in',     '$2a$10$igzRaSkBndd6mKrxDcFjtOSqelaobun/LOFvvfBQxR6BxkYKy.1va', 'HOD', 'HOD001', 'CSE'),
('Dr. Suresh Menon',      'hod.ece@tkmit.ac.in',      '$2a$10$igzRaSkBndd6mKrxDcFjtOSqelaobun/LOFvvfBQxR6BxkYKy.1va', 'HOD', 'HOD002', 'ECE'),
('Dr. Rajan Pillai',      'hod.mech@tkmit.ac.in',     '$2a$10$igzRaSkBndd6mKrxDcFjtOSqelaobun/LOFvvfBQxR6BxkYKy.1va', 'HOD', 'HOD003', 'MECH');

-- ─── FACULTY (CSE) ────────────────────────────────────────────────────────────
INSERT INTO users (name, email, password, role, employee_id, department) VALUES
('Prof. Meera Nair',      'meera.nair@tkmit.ac.in',   '$2a$10$igzRaSkBndd6mKrxDcFjtOSqelaobun/LOFvvfBQxR6BxkYKy.1va', 'FACULTY', 'FAC001', 'CSE'),
('Prof. Sreejith Kumar',  'sreejith.k@tkmit.ac.in',   '$2a$10$igzRaSkBndd6mKrxDcFjtOSqelaobun/LOFvvfBQxR6BxkYKy.1va', 'FACULTY', 'FAC002', 'CSE'),
('Prof. Divya Mohan',     'divya.mohan@tkmit.ac.in',  '$2a$10$igzRaSkBndd6mKrxDcFjtOSqelaobun/LOFvvfBQxR6BxkYKy.1va', 'FACULTY', 'FAC003', 'CSE');

-- ─── PLACEMENT OFFICER ────────────────────────────────────────────────────────
INSERT INTO users (name, email, password, role, employee_id, department) VALUES
('Mr. Ajay Thomas', 'placement@tkmit.ac.in',
 '$2a$10$igzRaSkBndd6mKrxDcFjtOSqelaobun/LOFvvfBQxR6BxkYKy.1va',
 'PLACEMENT_OFFICER', 'PLC001', 'Placement Cell');

-- ─── INDUSTRIAL CONNECT HEAD ──────────────────────────────────────────────────
INSERT INTO users (name, email, password, role, employee_id, department) VALUES
('Dr. Harish Kumar', 'ich@tkmit.ac.in',
 '$2a$10$igzRaSkBndd6mKrxDcFjtOSqelaobun/LOFvvfBQxR6BxkYKy.1va',
 'INDUSTRIAL_CONNECT_HEAD', 'ICH001', 'Industry Relations');

-- ─── STUDENTS (CSE S4) ────────────────────────────────────────────────────────
INSERT INTO users (name, email, password, role, roll_number, department, semester, section) VALUES
('Anzal Rahman',    'anzal.r@student.tkmit.ac.in',  '$2a$10$njczDlSm5D241JoQdF5X9.nn4kEzpYu7TQXOpP.k/5aEdB0p.HOY6', 'STUDENT', 'TKM22CS001', 'CSE', 'S4', 'A'),
('Hajira Fathima',  'hajira.f@student.tkmit.ac.in', '$2a$10$njczDlSm5D241JoQdF5X9.nn4kEzpYu7TQXOpP.k/5aEdB0p.HOY6', 'STUDENT', 'TKM22CS002', 'CSE', 'S4', 'A'),
('Arjun R',         'arjun.r@student.tkmit.ac.in',  '$2a$10$njczDlSm5D241JoQdF5X9.nn4kEzpYu7TQXOpP.k/5aEdB0p.HOY6', 'STUDENT', 'TKM22CS003', 'CSE', 'S4', 'A'),
('Riya Krishnan',   'riya.k@student.tkmit.ac.in',   '$2a$10$njczDlSm5D241JoQdF5X9.nn4kEzpYu7TQXOpP.k/5aEdB0p.HOY6', 'STUDENT', 'TKM22CS004', 'CSE', 'S4', 'B'),
('Arun Mohan',      'arun.m@student.tkmit.ac.in',   '$2a$10$njczDlSm5D241JoQdF5X9.nn4kEzpYu7TQXOpP.k/5aEdB0p.HOY6', 'STUDENT', 'TKM22CS005', 'CSE', 'S4', 'B'),
('Sreehari Dev',    'sreehari.d@student.tkmit.ac.in','$2a$10$njczDlSm5D241JoQdF5X9.nn4kEzpYu7TQXOpP.k/5aEdB0p.HOY6', 'STUDENT', 'TKM22CS006', 'CSE', 'S4', 'A');

-- ─── SUBJECTS (CSE S4) ────────────────────────────────────────────────────────
INSERT INTO subjects (name, code, department, semester, credits, faculty_id) VALUES
('Design and Analysis of Algorithms', 'CS401', 'CSE', 'S4', 4, 6),
('Database Management Systems',       'CS402', 'CSE', 'S4', 4, 7),
('Operating Systems',                 'CS403', 'CSE', 'S4', 3, 8),
('Computer Networks',                 'CS404', 'CSE', 'S4', 3, 6),
('Software Engineering',              'CS405', 'CSE', 'S4', 3, 7);

-- ─── NOTICES ─────────────────────────────────────────────────────────────────
INSERT INTO notices (title, content, category, visibility, created_by, expires_at) VALUES
('Internal Exam Schedule Released',
 'The schedule for S4 internal examinations has been published. Please check the timetable section for details. All students must report 15 minutes before the exam.',
 'ACADEMIC', 'ENTIRE_COLLEGE', 1, DATE_ADD(NOW(), INTERVAL 14 DAY)),

('NAAC Accreditation Visit — March 28',
 'NAAC peer team will be visiting TKMIT on March 28. All faculty and staff are expected to be present in formal attire. Labs and classrooms should be in order.',
 'ADMINISTRATIVE', 'FACULTY_ONLY', 2, DATE_ADD(NOW(), INTERVAL 10 DAY)),

('Fee Submission Deadline — March 31',
 'Students who have not yet paid their semester fees are reminded that the last date is March 31. Late fee fine will apply after this date.',
 'ADMINISTRATIVE', 'STUDENTS_ONLY', 1, DATE_ADD(NOW(), INTERVAL 15 DAY)),

('TCS Campus Drive — April 5',
 'TCS will be visiting campus for recruitment on April 5. Eligible students (CSE, ECE, 2025 batch, CGPA ≥ 6.0) should register with the Placement Cell by March 30.',
 'PLACEMENT', 'STUDENTS_ONLY', 10, DATE_ADD(NOW(), INTERVAL 20 DAY)),

('College Annual Day — April 12',
 'The Annual Day celebrations will be held on April 12 at the Main Auditorium. Cultural programs, prize distribution, and chief guest address from 5 PM onwards.',
 'EVENTS', 'ENTIRE_COLLEGE', 1, DATE_ADD(NOW(), INTERVAL 25 DAY));

-- ─── PLACEMENT DRIVES ────────────────────────────────────────────────────────
INSERT INTO placement_drives (company_name, logo, role, description, package_lpa, drive_date, last_date_to_apply, min_cgpa, eligible_branches, eligible_batch, job_type, location, status) VALUES
('TCS', '🔷', 'Software Engineer', 'Full-time software engineer role at TCS Digital. Training provided for 3 months at Trivandrum/Kochi. Good growth path.', 7.0, DATE_ADD(CURDATE(), INTERVAL 20 DAY), DATE_ADD(CURDATE(), INTERVAL 12 DAY), 6.0, 'CSE,ECE,EEE', '2025', 'Full-time', 'Thiruvananthapuram', 'UPCOMING'),

('Infosys', '🟣', 'Systems Engineer', 'Infosys Systems Engineer role. Multi-city posting. Part of InfyTQ campus program with 6-month training bond.', 6.5, DATE_ADD(CURDATE(), INTERVAL 30 DAY), DATE_ADD(CURDATE(), INTERVAL 22 DAY), 6.5, 'CSE,ECE,MECH', '2025', 'Full-time', 'Bengaluru / Pune', 'UPCOMING'),

('Wipro', '🟢', 'Project Engineer', 'Wipro Elite NTH (National Talent Hunt). 3-month training + project allocation. Hybrid work option after probation.', 6.5, DATE_ADD(CURDATE(), INTERVAL 45 DAY), DATE_ADD(CURDATE(), INTERVAL 35 DAY), 6.0, 'CSE,ECE,EEE,CE', '2025', 'Full-time', 'Kochi / Hyderabad', 'UPCOMING'),

('UST Global', '🔵', 'Associate Software Engineer', 'UST campus recruitment for Kerala students. Kochi-based. Immediate joining after training.', 5.5, DATE_ADD(CURDATE(), INTERVAL -5 DAY), DATE_ADD(CURDATE(), INTERVAL -12 DAY), 6.5, 'CSE,ECE', '2025', 'Full-time', 'Kochi', 'COMPLETED'),

('IBS Software', '🟠', 'Software Trainee', 'Thiruvananthapuram-based aviation & travel tech company. Known for good work culture in Kerala.', 5.0, DATE_ADD(CURDATE(), INTERVAL 60 DAY), DATE_ADD(CURDATE(), INTERVAL 50 DAY), 7.0, 'CSE', '2025', 'Full-time', 'Thiruvananthapuram', 'UPCOMING');

-- ─── SAMPLE LEAVE REQUESTS ────────────────────────────────────────────────────
INSERT INTO leave_requests (student_id, leave_type, reason, from_date, to_date, no_of_days, event_name, overall_status, faculty_status, hod_status, current_stage) VALUES
(10, 'DUTY_LEAVE',   'Representing college in state-level coding competition at CUSAT, Kochi.', DATE_ADD(CURDATE(), INTERVAL 3 DAY),  DATE_ADD(CURDATE(), INTERVAL 4 DAY),  2, 'State Coding Championship 2025', 'PENDING', 'PENDING', 'PENDING', 'FACULTY_REVIEW'),
(11, 'OD_LETTER',    'Attending IEEE sponsored workshop on Machine Learning at NIT Calicut.',    DATE_ADD(CURDATE(), INTERVAL 5 DAY),  DATE_ADD(CURDATE(), INTERVAL 5 DAY),  1, 'ML Workshop NIT Calicut',       'PENDING', 'PENDING', 'PENDING', 'FACULTY_REVIEW'),
(12, 'DUTY_LEAVE',   'Part of college team for inter-college cricket tournament in Kollam.',     DATE_ADD(CURDATE(), INTERVAL -5 DAY), DATE_ADD(CURDATE(), INTERVAL -3 DAY), 3, 'Inter-College Cricket',         'APPROVED', 'APPROVED', 'APPROVED', 'COMPLETED'),
(15, 'MEDICAL_LEAVE','Fever and throat infection. Doctor has advised rest for 2 days.',          DATE_ADD(CURDATE(), INTERVAL -2 DAY), DATE_ADD(CURDATE(), INTERVAL -1 DAY), 2, NULL,                           'APPROVED', 'APPROVED', 'APPROVED', 'COMPLETED');

-- ─── SAMPLE ATTENDANCE ───────────────────────────────────────────────────────
-- Anzal (student_id=10), Subject CS401 (subject_id=1)
INSERT INTO attendance (student_id, subject_id, faculty_id, date, status, period_number) VALUES
(10, 1, 6, DATE_SUB(CURDATE(), INTERVAL 5 DAY), 'PRESENT', 1),
(10, 1, 6, DATE_SUB(CURDATE(), INTERVAL 4 DAY), 'PRESENT', 1),
(10, 1, 6, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 'ABSENT',  1),
(10, 1, 6, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 'PRESENT', 1),
(10, 1, 6, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'PRESENT', 1),
-- CS402
(10, 2, 7, DATE_SUB(CURDATE(), INTERVAL 5 DAY), 'PRESENT', 2),
(10, 2, 7, DATE_SUB(CURDATE(), INTERVAL 4 DAY), 'ABSENT',  2),
(10, 2, 7, DATE_SUB(CURDATE(), INTERVAL 3 DAY), 'ABSENT',  2),
(10, 2, 7, DATE_SUB(CURDATE(), INTERVAL 2 DAY), 'PRESENT', 2),
(10, 2, 7, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'PRESENT', 2);

-- ─── SAMPLE MARKS ────────────────────────────────────────────────────────────
INSERT INTO marks (student_id, subject_id, exam_type, marks_obtained, max_marks, grade, semester, academic_year, entered_by) VALUES
(10, 1, 'INTERNAL_1', 38, 50, 'A', 'S4', '2025-2026', 'Prof. Meera Nair'),
(10, 2, 'INTERNAL_1', 42, 50, 'A+','S4', '2025-2026', 'Prof. Sreejith Kumar'),
(10, 3, 'INTERNAL_1', 35, 50, 'B+','S4', '2025-2026', 'Prof. Divya Mohan'),
(11, 1, 'INTERNAL_1', 44, 50, 'A+','S4', '2025-2026', 'Prof. Meera Nair'),
(11, 2, 'INTERNAL_1', 40, 50, 'A', 'S4', '2025-2026', 'Prof. Sreejith Kumar');

-- ─── TIMETABLE (CSE S4 A — sample) ──────────────────────────────────────────
INSERT INTO timetable (department, semester, section, day_of_week, period_number, start_time, end_time, subject_id, faculty_id, room) VALUES
('CSE','S4','A','MON',1,'09:00','09:50',1,6,'Room 301'),
('CSE','S4','A','MON',2,'09:50','10:40',2,7,'Room 301'),
('CSE','S4','A','MON',3,'10:50','11:40',3,8,'Room 301'),
('CSE','S4','A','MON',4,'11:40','12:30',4,6,'Room 301'),
('CSE','S4','A','TUE',1,'09:00','09:50',2,7,'Room 301'),
('CSE','S4','A','TUE',2,'09:50','10:40',5,7,'Room 301'),
('CSE','S4','A','WED',1,'09:00','09:50',1,6,'Room 301'),
('CSE','S4','A','WED',2,'09:50','10:40',3,8,'CS Lab 1'),
('CSE','S4','A','THU',1,'09:00','09:50',4,6,'Room 301'),
('CSE','S4','A','THU',2,'09:50','10:40',5,7,'Room 301'),
('CSE','S4','A','FRI',1,'09:00','09:50',2,7,'Room 301'),
('CSE','S4','A','FRI',2,'09:50','10:40',1,6,'Room 301');

-- ─── COMPANIES & MOUS (Sample Industry Partners) ─────────────────────────────
INSERT INTO companies (id, name, logo, location, description, sector, website, hr_name, hr_email, hr_phone, partnership_stage, tier, placement_ready) VALUES
(1, 'TechCorp Solutions', '🏢', 'Technopark, Thiruvananthapuram', 'Enterprise cloud computing, AI platforms, and IoT solutions partner.', 'Information Technology', 'https://techcorp.example.com', 'Sujith Kumar', 'sujith.hr@techcorp.example.com', '+91 98470 11223', 'ACTIVE', 'TIER_1', 1),
(2, 'InnovateX Labs', '💡', 'Infopark, Kochi', 'Robotics, automation, and advanced manufacturing research lab.', 'Automation & Robotics', 'https://innovatex.example.com', 'Divya R', 'careers@innovatex.example.com', '+91 94471 22334', 'MOU_SIGNED', 'TIER_2', 1),
(3, 'Kerala State Electronics (KELTRON)', '⚡', 'Karakulam, Thiruvananthapuram', 'State public sector enterprise in defense electronics and IT infrastructure.', 'Core Electronics', 'https://keltron.org', 'Narayanan N', 'training@keltron.org', '+91 471 2724444', 'ACTIVE', 'GOVT', 0);

INSERT INTO industry_mous (company_id, title, signed_date, valid_from, valid_to, scope, status, departments, remarks) VALUES
(1, 'Academic-Industry Partnership & Internship Alliance', CURDATE(), CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 YEAR), 'Internships, capstone project mentorship, cloud computing lab setup, and joint workshops.', 'ACTIVE', 'CSE,ECE', 'Renewed partnership with TechCorp Solutions.'),
(3, 'Public Sector Technical Training & Student Internships MoU', CURDATE(), CURDATE(), DATE_ADD(CURDATE(), INTERVAL 1 YEAR), 'Industrial visits, embedded systems hands-on training, and summer internships.', 'ACTIVE', 'ECE,EEE,MECH', 'Signed under state technical skill initiative.');

INSERT INTO internships (company_id, title, description, required_skills, duration, stipend, mode, departments, seats, deadline, status, location) VALUES
(1, 'Cloud Solutions & AI Intern', 'Hands-on experience deploying microservices, Docker, Spring Boot, and cloud telemetry.', 'Java, Spring Boot, Docker, SQL', '3 Months', '15000/month', 'HYBRID', 'CSE,ECE', 10, DATE_ADD(CURDATE(), INTERVAL 30 DAY), 'OPEN', 'Technopark, Thiruvananthapuram'),
(3, 'Embedded Systems & Firmware Trainee', 'Testing and programming embedded ARM microcontrollers and sensor telemetry.', 'C/C++, Microcontrollers, Circuit Design', '1 Month', '5000/month', 'ON_SITE', 'ECE,EEE', 15, DATE_ADD(CURDATE(), INTERVAL 25 DAY), 'OPEN', 'Thiruvananthapuram');


-- ═══════════════════════════════════════════════════════════════════
--  VERIFICATION — run these to confirm seed data loaded correctly
-- ═══════════════════════════════════════════════════════════════════
SELECT '──── USERS ────' AS '';
SELECT id, name, role, department FROM users ORDER BY id;

SELECT '──── SUBJECTS ────' AS '';
SELECT id, code, name, department, semester FROM subjects;

SELECT '──── NOTICES ────' AS '';
SELECT id, title, category, is_active FROM notices;

SELECT '──── PLACEMENT DRIVES ────' AS '';
SELECT id, company_name, role, package_lpa, status FROM placement_drives;

SELECT '──── LEAVE REQUESTS ────' AS '';
SELECT id, student_id, leave_type, overall_status, current_stage FROM leave_requests;

SELECT '──── COMPANIES & MOUS ────' AS '';
SELECT id, name, sector, partnership_stage, placement_ready FROM companies;
SELECT id, title, status, valid_to FROM industry_mous;
SELECT id, title, seats, deadline, status FROM internships;
