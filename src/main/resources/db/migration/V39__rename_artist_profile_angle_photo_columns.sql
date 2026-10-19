ALTER TABLE artist_profiles
    CHANGE COLUMN front_profile_photo_url front_profile_photo VARCHAR(2048) NULL,
    CHANGE COLUMN left_profile_photo_url left_profile_photo VARCHAR(2048) NULL,
    CHANGE COLUMN right_profile_photo_url right_profile_photo VARCHAR(2048) NULL;
