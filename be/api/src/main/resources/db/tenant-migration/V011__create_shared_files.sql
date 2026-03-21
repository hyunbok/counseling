CREATE TABLE shared_files (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id        UUID         NOT NULL REFERENCES channels(id),
    uploader_id       VARCHAR(100) NOT NULL,
    uploader_type     VARCHAR(20)  NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename   VARCHAR(255) NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    file_size         BIGINT       NOT NULL,
    storage_path      TEXT         NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted           BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_shared_files_channel ON shared_files (channel_id, created_at DESC) WHERE deleted = FALSE;
