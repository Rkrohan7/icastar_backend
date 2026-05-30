-- Add Super Admin User
-- Email: rohanravikadam@gmail.com
-- Password: Rohan@123 (BCrypt encoded)

INSERT INTO users (
    email,
    mobile,
    first_name,
    last_name,
    password,
    role,
    status,
    is_verified,
    is_onboarding_complete,
    account_status,
    is_active,
    failed_login_attempts,
    login_attempts,
    created_at,
    updated_at
) VALUES (
    'rohanravikadam@gmail.com',
    '+919999999999',
    'Rohan',
    'Kadam',
    '$2a$10$.ygwZ3CGnkC/IRCowbznTeCNUc5ADm.3cbP5ttQfxQXhhw7tRE0Wa',
    'ADMIN',
    'ACTIVE',
    true,
    true,
    'ACTIVE',
    true,
    0,
    0,
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE
    password = '$2a$10$.ygwZ3CGnkC/IRCowbznTeCNUc5ADm.3cbP5ttQfxQXhhw7tRE0Wa',
    role = 'ADMIN',
    status = 'ACTIVE',
    is_verified = true,
    account_status = 'ACTIVE',
    updated_at = NOW();

-- Verify the user was added
SELECT id, email, first_name, last_name, role, status, is_verified, account_status
FROM users
WHERE email = 'rohanravikadam@gmail.com';
