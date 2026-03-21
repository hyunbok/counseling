CREATE TABLE screen_captures (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id        UUID         NOT NULL REFERENCES channels(id),
    captured_by       UUID         NOT NULL REFERENCES agents(id),
    original_filename VARCHAR(255) NOT NULL,
    stored_filename   VARCHAR(255) NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    file_size         BIGINT       NOT NULL,
    storage_path      TEXT         NOT NULL,
    note              VARCHAR(500),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted           BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_screen_captures_channel ON screen_captures (channel_id, created_at DESC) WHERE deleted = FALSE;
