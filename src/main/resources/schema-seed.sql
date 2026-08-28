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
    role           ENUM('STUDENT','FACULTY','HOD','PRINCIPAL','PLACEMENT_OFFICER','ADMIN') NOT NULL,
    roll_number    VARCHAR(30),
    employee_id    VARCHAR(30),
    department     VARCHAR(50),
    semester       VARCHAR(10),
    section        VARCHAR(5),
    phone          VARCHAR(20),
    profile_pic    VARCHAR(255),
    is_active      TINYINT(1)   DEFAULT 1,
    created_at     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
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
