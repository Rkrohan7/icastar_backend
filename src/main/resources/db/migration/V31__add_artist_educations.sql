-- Migration V31: Add artist_educations table

CREATE TABLE artist_educations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    artist_profile_id BIGINT NOT NULL,
    education_level ENUM('SCHOOL_10TH','HIGHER_SECONDARY_12TH','DIPLOMA','GRADUATION',
                         'POST_GRADUATION','DOCTORATE','CERTIFICATION','OTHER') NOT NULL,
    course_name VARCHAR(150) NOT NULL,
    specialization VARCHAR(100),
    institution VARCHAR(150) NOT NULL,
    course_type ENUM('FULL_TIME','PART_TIME','DISTANCE'),
    is_pursuing BOOLEAN DEFAULT FALSE,
    start_year INT,
    end_year INT,
    grade VARCHAR(30),
    description VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_education_artist FOREIGN KEY (artist_profile_id) REFERENCES artist_profiles(id) ON DELETE CASCADE,
    INDEX idx_education_artist (artist_profile_id)
);