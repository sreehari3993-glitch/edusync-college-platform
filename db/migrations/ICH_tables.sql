-- =============================================================================
-- Migration: db/migrations/ICH_tables.sql
-- Description: DDL schema for Industrial Connect Head (ICH) module.
-- Note: Do NOT execute directly if Hibernate ddl-auto=update is active.
-- =============================================================================

-- 1. COMPANIES (Shared Partner Registry)
CREATE TABLE IF NOT EXISTS `companies` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(255) NOT NULL UNIQUE,
    `logo` VARCHAR(255) NULL,
    `location` VARCHAR(255) NULL,
    `description` TEXT NULL,
    `sector` VARCHAR(150) NULL,
    `website` VARCHAR(255) NULL,
    `hr_name` VARCHAR(150) NULL,
    `hr_email` VARCHAR(150) NULL,
    `hr_phone` VARCHAR(50) NULL,
    `partnership_stage` VARCHAR(50) NOT NULL DEFAULT 'LEAD',
    `tier` VARCHAR(50) NULL,
    `placement_ready` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_companies_sector` (`sector`),
    INDEX `idx_companies_stage` (`partnership_stage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. INDUSTRY MOUs (Memorandums of Understanding)
CREATE TABLE IF NOT EXISTS `industry_mous` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `company_id` BIGINT NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `signed_date` DATE NULL,
    `valid_from` DATE NULL,
    `valid_to` DATE NULL,
    `scope` TEXT NULL,
    `status` VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    `drive_file_id` VARCHAR(255) NULL,
    `departments` VARCHAR(255) NULL,
    `document_url` VARCHAR(500) NULL,
    `remarks` TEXT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_mou_company` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`) ON DELETE RESTRICT,
    INDEX `idx_mou_status` (`status`),
    INDEX `idx_mou_valid_to` (`valid_to`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. INTERNSHIPS (Industry Sourced Internships)
CREATE TABLE IF NOT EXISTS `internships` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `company_id` BIGINT NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `description` TEXT NULL,
    `required_skills` TEXT NULL,
    `duration` VARCHAR(100) NULL,
    `stipend` VARCHAR(100) NULL,
    `mode` VARCHAR(50) NULL,
    `departments` VARCHAR(255) NULL,
    `seats` INT NULL,
    `deadline` DATE NULL,
    `status` VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    `location` VARCHAR(255) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_internship_company` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`) ON DELETE RESTRICT,
    INDEX `idx_internship_status` (`status`),
    INDEX `idx_internship_deadline` (`deadline`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. INTERNSHIP APPLICATIONS
CREATE TABLE IF NOT EXISTS `internship_applications` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `internship_id` BIGINT NOT NULL,
    `student_id` BIGINT NOT NULL,
    `resume_url` VARCHAR(500) NULL,
    `status` VARCHAR(50) NOT NULL DEFAULT 'APPLIED',
    `certificate_url` VARCHAR(500) NULL,
    `feedback` TEXT NULL,
    `applied_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_ia_internship` FOREIGN KEY (`internship_id`) REFERENCES `internships` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ia_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `uk_ia_student_internship` UNIQUE (`internship_id`, `student_id`),
    INDEX `idx_ia_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. INDUSTRY VISITS (Multi-Stage Approval Pipeline)
CREATE TABLE IF NOT EXISTS `industry_visits` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `company_id` BIGINT NOT NULL,
    `proposed_by_id` BIGINT NOT NULL,
    `department` VARCHAR(100) NOT NULL,
    `date` DATE NOT NULL,
    `student_count` INT NULL,
    `purpose` TEXT NULL,
    `location` VARCHAR(255) NULL,
    `overnight` BOOLEAN NOT NULL DEFAULT FALSE,
    `outside_kerala` BOOLEAN NOT NULL DEFAULT FALSE,
    `stage` VARCHAR(50) NOT NULL DEFAULT 'ICH_REVIEW',
    `status` VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    `ich_status` VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    `ich_remark` VARCHAR(500) NULL,
    `ich_approved_by` VARCHAR(150) NULL,
    `ich_approved_at` DATETIME NULL,
    `hod_status` VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    `hod_remark` VARCHAR(500) NULL,
    `hod_approved_by` VARCHAR(150) NULL,
    `hod_approved_at` DATETIME NULL,
    `principal_status` VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    `principal_remark` VARCHAR(500) NULL,
    `principal_approved_at` DATETIME NULL,
    `pdf_path` VARCHAR(500) NULL,
    `report_drive_file_id` VARCHAR(255) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_visit_company` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_visit_proposer` FOREIGN KEY (`proposed_by_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT,
    INDEX `idx_visit_stage` (`stage`),
    INDEX `idx_visit_status` (`status`),
    INDEX `idx_visit_dept` (`department`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. GUEST LECTURES & EXPERT SESSIONS
CREATE TABLE IF NOT EXISTS `guest_lectures` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `speaker` VARCHAR(150) NOT NULL,
    `designation` VARCHAR(150) NULL,
    `company_id` BIGINT NULL,
    `company_name` VARCHAR(255) NULL,
    `topic` VARCHAR(255) NOT NULL,
    `date` DATE NOT NULL,
    `time` VARCHAR(50) NULL,
    `venue` VARCHAR(255) NULL,
    `departments` VARCHAR(255) NULL,
    `attendance` INT NULL,
    `feedback_avg` DOUBLE NULL,
    `coordinator_id` BIGINT NULL,
    `status` VARCHAR(50) NOT NULL DEFAULT 'UPCOMING',
    `drive_file_id` VARCHAR(255) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_lecture_company` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_lecture_coordinator` FOREIGN KEY (`coordinator_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
    INDEX `idx_lecture_status` (`status`),
    INDEX `idx_lecture_date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. INDUSTRY PROJECTS (Capstone & Sponsored Consultancy)
CREATE TABLE IF NOT EXISTS `industry_projects` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `company_id` BIGINT NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `problem_statement` TEXT NOT NULL,
    `department` VARCHAR(100) NULL,
    `mentor` VARCHAR(150) NULL,
    `team_students` TEXT NULL,
    `status` VARCHAR(50) NOT NULL DEFAULT 'PROPOSED',
    `outcome` TEXT NULL,
    `academic_year` VARCHAR(50) NULL,
    `drive_file_id` VARCHAR(255) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_project_company` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`) ON DELETE RESTRICT,
    INDEX `idx_project_status` (`status`),
    INDEX `idx_project_dept` (`department`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. ADVISORY BOARD MEMBERS
CREATE TABLE IF NOT EXISTS `advisory_members` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(150) NOT NULL,
    `designation` VARCHAR(150) NULL,
    `company_id` BIGINT NULL,
    `company_name` VARCHAR(255) NULL,
    `email` VARCHAR(150) NULL,
    `phone` VARCHAR(50) NULL,
    `domain` VARCHAR(150) NULL,
    `bio` TEXT NULL,
    `tenure_start` DATE NULL,
    `tenure_end` DATE NULL,
    `status` VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_advisory_company` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`) ON DELETE SET NULL,
    INDEX `idx_advisory_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. CORPORATE ALUMNI CONTACTS
CREATE TABLE IF NOT EXISTS `alumni_contacts` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(150) NOT NULL,
    `batch` VARCHAR(50) NULL,
    `degree` VARCHAR(100) NULL,
    `department` VARCHAR(100) NULL,
    `current_company` VARCHAR(255) NULL,
    `current_role` VARCHAR(150) NULL,
    `email` VARCHAR(150) NULL,
    `phone` VARCHAR(50) NULL,
    `linkedin_url` VARCHAR(500) NULL,
    `willing_to_mentor` BOOLEAN NOT NULL DEFAULT TRUE,
    `willing_to_refer` BOOLEAN NOT NULL DEFAULT TRUE,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_alumni_company` (`current_company`),
    INDEX `idx_alumni_dept` (`department`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. INDUSTRY ACTIVITY LOGS (Audit Trail)
CREATE TABLE IF NOT EXISTS `industry_activity_logs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `actor` VARCHAR(150) NOT NULL,
    `action` VARCHAR(100) NOT NULL,
    `entity_type` VARCHAR(100) NOT NULL,
    `entity_id` BIGINT NULL,
    `details` TEXT NULL,
    `timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_activity_entity` (`entity_type`, `entity_id`),
    INDEX `idx_activity_time` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
