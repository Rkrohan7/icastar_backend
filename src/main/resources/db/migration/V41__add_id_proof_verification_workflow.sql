ALTER TABLE artist_profiles
    ADD COLUMN id_proof_verification_status VARCHAR(32) NOT NULL DEFAULT 'NOT_SUBMITTED',
    ADD COLUMN id_proof_verification_submitted_at DATETIME NULL,
    ADD COLUMN id_proof_verification_reason TEXT NULL;

UPDATE artist_profiles
SET id_proof_verification_status = CASE
        WHEN id_proof_url IS NULL THEN 'NOT_SUBMITTED'
        WHEN id_proof_verified = TRUE THEN 'APPROVED'
        WHEN id_proof_url IS NOT NULL THEN 'PENDING'
        ELSE 'NOT_SUBMITTED'
    END,
    id_proof_verification_submitted_at = CASE
        WHEN id_proof_url IS NOT NULL THEN COALESCE(id_proof_uploaded_at, updated_at)
        ELSE NULL
    END;

UPDATE artist_profiles
SET is_verified_badge = FALSE,
    verification_approved_at = NULL;
