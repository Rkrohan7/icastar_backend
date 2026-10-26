ALTER TABLE recruiter_profiles
    ADD COLUMN recruiter_type VARCHAR(100) NULL;

UPDATE recruiter_profiles rp
JOIN recruiter_categories rc ON rc.id = rp.recruiter_category_id
SET rp.recruiter_type = rc.name
WHERE rp.recruiter_type IS NULL;
