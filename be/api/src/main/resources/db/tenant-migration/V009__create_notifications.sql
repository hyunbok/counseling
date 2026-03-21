CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id    UUID NOT NULL,
    recipient_type  VARCHAR(20) NOT NULL,
    type            VARCHAR(40) NOT NULL,
    title           VARCHAR(200) NOT NULL,
    body            TEXT NOT NULL,
    reference_id    UUID,
    reference_type  VARCHAR(30),
    delivery_method VARCHAR(10) NOT NULL DEFAULT 'IN_APP',
    read            BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_notifications_recipient_unread ON notifications (recipient_id, created_at DESC) WHERE deleted = FALSE AND read = FALSE;
CREATE INDEX idx_notifications_recipient_all ON notifications (recipient_id, created_at DESC) WHERE deleted = FALSE;
