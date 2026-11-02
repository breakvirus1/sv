-- ============================================================
-- V8: Недостающие объекты схемы comment-service для чистой БД.
-- images / notifications / связующие таблицы раньше создавал Hibernate
-- (ddl-auto=update в local-профиле в схему public). В docker-профиле
-- ddl-auto=none, поэтому схема должна полностью приходить из миграций.
-- Идемпотентно: только CREATE IF NOT EXISTS и ADD COLUMN IF NOT EXISTS.
-- ============================================================

CREATE SCHEMA IF NOT EXISTS svschema;

-- Колонки сущностей, отсутствующие в V1/V2
ALTER TABLE svschema.comments
    ADD COLUMN IF NOT EXISTS employee_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS company_id BIGINT;

ALTER TABLE svschema.comment_replies
    ADD COLUMN IF NOT EXISTS employee_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS company_id BIGINT;

-- Вложения комментариев и ответов
CREATE TABLE IF NOT EXISTS svschema.images (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    filename VARCHAR(255) NOT NULL UNIQUE,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    size BIGINT,
    path VARCHAR(255) NOT NULL,
    company_id BIGINT
);

CREATE TABLE IF NOT EXISTS svschema.comment_images (
    comment_id BIGINT NOT NULL REFERENCES svschema.comments (id) ON DELETE CASCADE,
    image_id BIGINT NOT NULL REFERENCES svschema.images (id) ON DELETE CASCADE,
    PRIMARY KEY (comment_id, image_id)
);

CREATE TABLE IF NOT EXISTS svschema.reply_images (
    reply_id BIGINT NOT NULL REFERENCES svschema.comment_replies (id) ON DELETE CASCADE,
    image_id BIGINT NOT NULL REFERENCES svschema.images (id) ON DELETE CASCADE,
    PRIMARY KEY (reply_id, image_id)
);

-- Уведомления
CREATE TABLE IF NOT EXISTS svschema.notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    message TEXT,
    type VARCHAR(255) NOT NULL,
    reference_id BIGINT,
    reference_type VARCHAR(255),
    reference_sub_id BIGINT,
    readed BOOLEAN DEFAULT FALSE,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    company_id BIGINT
);

CREATE INDEX IF NOT EXISTS idx_comments_order_id ON svschema.comments (order_id);
CREATE INDEX IF NOT EXISTS idx_comments_company_id ON svschema.comments (company_id);
CREATE INDEX IF NOT EXISTS idx_comment_replies_order_id ON svschema.comment_replies (order_id);
CREATE INDEX IF NOT EXISTS idx_comment_replies_parent_comment_id ON svschema.comment_replies (parent_comment_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON svschema.notifications (user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_company_id ON svschema.notifications (company_id);
