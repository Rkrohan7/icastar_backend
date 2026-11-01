ALTER TABLE users
    ADD COLUMN admin_created_by_id BIGINT NULL,
    ADD COLUMN admin_updated_by_id BIGINT NULL;
