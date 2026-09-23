-- V36: Widen mobile column for soft delete suffix
-- mobile + _deleted_ + timestamp needs more than 15 chars

ALTER TABLE users MODIFY COLUMN mobile VARCHAR(64) NOT NULL;
