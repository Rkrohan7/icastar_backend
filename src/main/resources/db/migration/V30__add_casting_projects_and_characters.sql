-- Migration V30: Add casting_projects and casting_characters tables
-- Also add project_id, character_id, role_type to jobs table

-- Create casting_projects table
CREATE TABLE casting_projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recruiter_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    project_type ENUM('FEATURE_FILM', 'TV_SERIES', 'WEB_SERIES', 'SHORT_FILM', 'COMMERCIAL', 'MUSIC_VIDEO', 'THEATER') NOT NULL,
    production_house VARCHAR(255),
    director VARCHAR(255),
    language VARCHAR(100),
    shoot_location VARCHAR(255),
    shoot_start_date DATE,
    shoot_end_date DATE,
    description TEXT,
    status ENUM('ACTIVE', 'COMPLETED', 'ON_HOLD') DEFAULT 'ACTIVE',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_casting_project_recruiter FOREIGN KEY (recruiter_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_casting_project_recruiter (recruiter_id),
    INDEX idx_casting_project_status (status)
);

-- Create casting_characters table
CREATE TABLE casting_characters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    role_type ENUM('LEAD', 'SUPPORTING', 'BACKGROUND', 'EXTRA') NOT NULL,
    gender ENUM('MALE', 'FEMALE', 'NON_BINARY', 'ANY') DEFAULT 'ANY',
    age_min INT,
    age_max INT,
    description TEXT,
    required_count INT DEFAULT 1,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_casting_character_project FOREIGN KEY (project_id) REFERENCES casting_projects(id) ON DELETE CASCADE,
    INDEX idx_casting_character_project (project_id)
);

-- Add new columns to jobs table (nullable for existing jobs)
ALTER TABLE jobs
ADD COLUMN project_id BIGINT NULL,
ADD COLUMN character_id BIGINT NULL,
ADD COLUMN role_type ENUM('LEAD', 'SUPPORTING', 'BACKGROUND', 'EXTRA') NULL;

-- Add foreign key constraints
ALTER TABLE jobs
ADD CONSTRAINT fk_job_project FOREIGN KEY (project_id) REFERENCES casting_projects(id) ON DELETE SET NULL,
ADD CONSTRAINT fk_job_character FOREIGN KEY (character_id) REFERENCES casting_characters(id) ON DELETE SET NULL;

-- Add indexes for the new columns
ALTER TABLE jobs
ADD INDEX idx_job_project (project_id),
ADD INDEX idx_job_character (character_id);