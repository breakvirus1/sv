CREATE SCHEMA IF NOT EXISTS svschema;

CREATE TABLE IF NOT EXISTS svschema.comments (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    deleted BOOLEAN DEFAULT FALSE,
    order_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    readed BOOLEAN DEFAULT FALSE,
    body TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_comments_order_id ON svschema.comments(order_id);
CREATE INDEX IF NOT EXISTS idx_comments_employee_id ON svschema.comments(employee_id);
CREATE INDEX IF NOT EXISTS idx_comments_deleted ON svschema.comments(deleted) WHERE deleted = FALSE;
