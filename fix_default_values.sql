-- Fix missing default values for boolean columns in users table
-- Run this script on the icastar_dev database

USE icastar_dev;

-- Update is_active column to have default value
ALTER TABLE users MODIFY COLUMN is_active BOOLEAN NOT NULL DEFAULT true;

-- Update is_verified column to have default value
ALTER TABLE users MODIFY COLUMN is_verified BOOLEAN NOT NULL DEFAULT false;

-- Update is_onboarding_complete column to have default value
ALTER TABLE users MODIFY COLUMN is_onboarding_complete BOOLEAN NOT NULL DEFAULT false;

-- Optional: Update any existing NULL values (if any exist)
UPDATE users SET is_active = true WHERE is_active IS NULL;
UPDATE users SET is_verified = false WHERE is_verified IS NULL;
UPDATE users SET is_onboarding_complete = false WHERE is_onboarding_complete IS NULL;

-- Verify the changes
SHOW CREATE TABLE users;
 