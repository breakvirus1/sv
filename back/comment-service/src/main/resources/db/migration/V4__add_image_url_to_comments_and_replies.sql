ALTER TABLE svschema.comments
    ADD COLUMN IF NOT EXISTS image_url TEXT;

ALTER TABLE svschema.comment_replies
    ADD COLUMN IF NOT EXISTS image_url TEXT;
