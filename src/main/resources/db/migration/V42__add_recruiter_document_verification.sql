ALTER TABLE recruiter_profiles
    ADD COLUMN face_verification_url VARCHAR(2048) NULL,
    ADD COLUMN face_verification_status VARCHAR(32) NOT NULL DEFAULT 'NOT_SUBMITTED',
    ADD COLUMN is_face_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN face_verification_reason TEXT NULL,
    ADD COLUMN id_proof_url VARCHAR(2048) NULL,
    ADD COLUMN id_proof_verification_status VARCHAR(32) NOT NULL DEFAULT 'NOT_SUBMITTED',
    ADD COLUMN id_proof_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN id_proof_verification_reason TEXT NULL;
