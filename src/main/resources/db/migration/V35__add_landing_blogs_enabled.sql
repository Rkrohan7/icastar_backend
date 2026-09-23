-- V35: Add landingBlogsEnabled setting

INSERT INTO system_settings (setting_key, setting_value, category) VALUES
  ('landingBlogsEnabled', 'true', 'LANDING_STATS');
