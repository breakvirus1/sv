CREATE TABLE IF NOT EXISTS svschema.comment_replies (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted BOOLEAN DEFAULT FALSE,
    order_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    readed BOOLEAN DEFAULT FALSE,
    body TEXT NOT NULL,
    parent_comment_id BIGINT
);

CREATE INDEX IF NOT EXISTS idx_comment_replies_parent_comment_id ON svschema.comment_replies(parent_comment_id);
CREATE INDEX IF NOT EXISTS idx_comment_replies_order_id ON svschema.comment_replies(order_id);
CREATE INDEX IF NOT EXISTS idx_comment_replies_employee_id ON svschema.comment_replies(employee_id);
CREATE INDEX IF NOT EXISTS idx_comment_replies_deleted ON svschema.comment_replies(deleted) WHERE deleted = FALSE;
