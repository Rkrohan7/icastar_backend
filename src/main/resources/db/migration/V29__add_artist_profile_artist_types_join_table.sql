-- Migration V29: Add many-to-many relationship for artist types
-- This allows artists to have multiple professions/artist types

-- Create the join table for artist profiles and artist types
CREATE TABLE artist_profile_artist_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    artist_profile_id BIGINT NOT NULL,
    artist_type_id BIGINT NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Foreign keys
    CONSTRAINT fk_apat_artist_profile FOREIGN KEY (artist_profile_id)
        REFERENCES artist_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_apat_artist_type FOREIGN KEY (artist_type_id)
        REFERENCES artist_types(id) ON DELETE CASCADE,

    -- Unique constraint: each artist can have each type only once
    CONSTRAINT uk_artist_profile_artist_type UNIQUE (artist_profile_id, artist_type_id),

    -- Index for faster lookups
    INDEX idx_apat_artist_profile (artist_profile_id),
    INDEX idx_apat_artist_type (artist_type_id),
    INDEX idx_apat_primary (artist_profile_id, is_primary)
);

-- Migrate existing data: insert current artist_type_id as the primary type
INSERT INTO artist_profile_artist_types (artist_profile_id, artist_type_id, is_primary, sort_order)
SELECT id, artist_type_id, TRUE, 0
FROM artist_profiles
WHERE artist_type_id IS NOT NULL;

-- Note: We keep the artist_type_id column in artist_profiles for backward compatibility
-- and as a denormalized reference to the primary profession for faster queries.
-- The join table is the source of truth for all artist types.
