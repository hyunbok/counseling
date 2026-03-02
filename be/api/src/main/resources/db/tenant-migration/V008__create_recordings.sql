CREATE TABLE recordings (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id  UUID        NOT NULL REFERENCES channels(id),
    egress_id   VARCHAR(100) NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'RECORDING',
    file_path   TEXT,
    started_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    stopped_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted     BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_recordings_status CHECK (status IN ('RECORDING', 'STOPPED', 'FAILED'))
);
CREATE INDEX idx_recordings_channel ON recordings (channel_id) WHERE deleted = FALSE;
CREATE UNIQUE INDEX idx_recordings_active ON recordings (channel_id) WHERE deleted = FALSE AND status = 'RECORDING';
