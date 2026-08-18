-- Add guest applicant fields to job_applications table for public link applications
-- This allows applications without requiring a user login

-- Make artist_id nullable (for guest applications)
ALTER TABLE job_applications MODIFY COLUMN artist_id BIGINT NULL;

-- Add guest applicant fields
ALTER TABLE job_applications ADD COLUMN guest_full_name VARCHAR(255) NULL;
ALTER TABLE job_applications ADD COLUMN guest_email VARCHAR(255) NULL;
ALTER TABLE job_applications ADD COLUMN guest_phone VARCHAR(20) NULL;
ALTER TABLE job_applications ADD COLUMN guest_address VARCHAR(500) NULL;
ALTER TABLE job_applications ADD COLUMN guest_experience_years INT NULL;

-- Add application source field (PLATFORM or PUBLIC_LINK)
ALTER TABLE job_applications ADD COLUMN source VARCHAR(20) DEFAULT 'PLATFORM';

-- Add index on guest_email and guest_phone for duplicate checking
CREATE INDEX idx_job_applications_guest_email ON job_applications(guest_email);
CREATE INDEX idx_job_applications_guest_phone ON job_applications(guest_phone);
CREATE INDEX idx_job_applications_source ON job_applications(source);