ALTER TABLE auth_verification_challenges
    ADD COLUMN password_policy_validated BOOLEAN NOT NULL DEFAULT FALSE;
