-- V37: Add Terms & Conditions acceptance tracking columns
-- These columns track when and which version of Terms a user accepted at sign-up

ALTER TABLE users
    ADD COLUMN terms_accepted_at DATETIME(6) NULL,
    ADD COLUMN terms_version VARCHAR(20) NULL;