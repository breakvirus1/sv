ALTER TABLE svschema.comment_replies
    ADD COLUMN IF NOT EXISTS parent_reply_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_comment_replies_parent_reply_id
    ON svschema.comment_replies(parent_reply_id);
