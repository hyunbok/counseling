CREATE TABLE chat_messages (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id  UUID        NOT NULL REFERENCES channels(id),
    sender_type VARCHAR(20) NOT NULL,
    sender_id   VARCHAR(100) NOT NULL,
    content     TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_chat_messages_sender_type CHECK (sender_type IN ('AGENT', 'CUSTOMER'))
);
CREATE INDEX idx_chat_messages_channel ON chat_messages (channel_id, created_at);
