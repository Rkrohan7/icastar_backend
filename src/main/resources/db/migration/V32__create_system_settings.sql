-- V32: Create system_settings table for storing admin configuration

CREATE TABLE system_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value VARCHAR(1000),
    category VARCHAR(50),
    updated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_setting_key (setting_key),
    INDEX idx_setting_category (category)
);

-- Insert default landing page stats
INSERT INTO system_settings (setting_key, setting_value, category) VALUES
  ('landingActiveArtists',       '10000', 'LANDING_STATS'),
  ('landingCastingDirectors',    '250',   'LANDING_STATS'),
  ('landingSuccessfulAuditions', '10000', 'LANDING_STATS'),
  ('landingSuccessRate',         '95',    'LANDING_STATS');
