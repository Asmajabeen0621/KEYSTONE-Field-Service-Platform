CREATE TABLE notifications (id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id), message VARCHAR(1000) NOT NULL, read_flag BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMPTZ NOT NULL);
CREATE INDEX idx_notifications_user ON notifications(user_id, created_at DESC);
CREATE TABLE attachments (id BIGSERIAL PRIMARY KEY, work_order_id BIGINT NOT NULL REFERENCES work_orders(id), uploaded_by_id BIGINT NOT NULL REFERENCES users(id), original_name VARCHAR(255) NOT NULL, stored_name VARCHAR(255) NOT NULL, content_type VARCHAR(255) NOT NULL, size_bytes BIGINT NOT NULL, uploaded_at TIMESTAMPTZ NOT NULL);
CREATE INDEX idx_attachments_work_order ON attachments(work_order_id);
