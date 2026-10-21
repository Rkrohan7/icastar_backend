ALTER TABLE artist_profiles
    MODIFY COLUMN face_verification_url VARCHAR(2048) NULL,
    ADD COLUMN face_verification_status VARCHAR(32) NOT NULL DEFAULT 'NOT_SUBMITTED',
    ADD COLUMN is_face_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN face_verification_submitted_at DATETIME NULL,
    ADD COLUMN face_verification_reason TEXT NULL;

UPDATE artist_profiles
SET face_verification_status = 'PENDING',
    face_verification_submitted_at = updated_at
WHERE face_verification_url IS NOT NULL;

UPDATE artist_profiles
SET is_verified_badge = FALSE,
    verification_approved_at = NULL;
