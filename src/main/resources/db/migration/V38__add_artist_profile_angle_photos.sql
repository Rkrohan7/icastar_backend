ALTER TABLE artist_profiles
    ADD COLUMN front_profile_photo_url VARCHAR(2048) NULL,
    ADD COLUMN left_profile_photo_url VARCHAR(2048) NULL,
    ADD COLUMN right_profile_photo_url VARCHAR(2048) NULL;
