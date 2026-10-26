ALTER TABLE users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN mobile_verified BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE auth_verification_challenges (
    challenge_id VARCHAR(36) PRIMARY KEY,
    intent VARCHAR(16) NOT NULL,
    email VARCHAR(255) NOT NULL,
    mobile VARCHAR(20) NOT NULL,
    password_hash VARCHAR(100),
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    role VARCHAR(20),
    accepted_terms BOOLEAN,
    terms_version VARCHAR(20),
    email_otp_hash VARCHAR(100) NOT NULL,
    mobile_otp_hash VARCHAR(100),
    expires_at TIMESTAMP NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    resend_count INT NOT NULL DEFAULT 0,
    last_resend_at TIMESTAMP NULL,
    consumed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_auth_challenge_email_intent_created (email, intent, created_at),
    INDEX idx_auth_challenge_mobile_intent_created (mobile, intent, created_at)
);
