CREATE TABLE channels (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id       UUID        REFERENCES agents(id),
    status         VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    started_at     TIMESTAMPTZ,
    ended_at       TIMESTAMPTZ,
    recording_path TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted        BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_channels_status CHECK (status IN ('WAITING', 'IN_PROGRESS', 'CLOSED'))
);
CREATE INDEX idx_channels_agent_active ON channels (agent_id) WHERE deleted = FALSE;
CREATE INDEX idx_channels_status_active ON channels (status) WHERE deleted = FALSE;

CREATE TABLE endpoints (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id       UUID        NOT NULL REFERENCES channels(id),
    type             VARCHAR(20) NOT NULL,
    customer_name    VARCHAR(100),
    customer_contact VARCHAR(100),
    joined_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    left_at          TIMESTAMPTZ,
    CONSTRAINT ck_endpoints_type CHECK (type IN ('AGENT', 'CUSTOMER'))
);
CREATE INDEX idx_endpoints_channel ON endpoints (channel_id);
