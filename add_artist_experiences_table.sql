-- Artist Experiences Table
-- Run this SQL script to create the artist_experiences table

CREATE TABLE IF NOT EXISTS artist_experiences (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  artist_profile_id BIGINT NOT NULL,
  artist_type_id BIGINT NULL,            -- कोणत्या profession साठी
  title VARCHAR(100) NOT NULL,
  company_name VARCHAR(150) NOT NULL,
  project_type VARCHAR(50) NULL,
  employment_type VARCHAR(20) NULL,      -- FULL_TIME, PART_TIME, FREELANCE, CONTRACT, INTERNSHIP
  location VARCHAR(100) NULL,
  is_current BOOLEAN NOT NULL DEFAULT FALSE,
  start_date DATE NOT NULL,              -- नेहमी महिन्याचा 1 तारीख
  end_date DATE NULL,                    -- is_current = true असेल तर NULL
  description VARCHAR(1000) NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (artist_profile_id) REFERENCES artist_profiles(id) ON DELETE CASCADE,
  FOREIGN KEY (artist_type_id) REFERENCES artist_types(id) ON DELETE SET NULL
);

-- Index for faster queries
CREATE INDEX idx_artist_experiences_profile_id ON artist_experiences(artist_profile_id);
CREATE INDEX idx_artist_experiences_artist_type_id ON artist_experiences(artist_type_id);
CREATE INDEX idx_artist_experiences_start_date ON artist_experiences(start_date DESC);
