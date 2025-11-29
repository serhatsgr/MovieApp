ALTER TABLE comments
    ADD COLUMN parent_comment_id BIGINT REFERENCES comments(id),
    ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;