CREATE TABLE co_browsing_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id UUID NOT NULL REFERENCES channels(id),
    initiated_by UUID NOT NULL REFERENCES agents(id),
    status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_co_browsing_status CHECK (status IN ('REQUESTED','ACTIVE','ENDED'))
);
CREATE INDEX idx_co_browsing_channel ON co_browsing_sessions (channel_id, created_at DESC) WHERE NOT deleted;
CREATE UNIQUE INDEX idx_co_browsing_active ON co_browsing_sessions (channel_id) WHERE NOT deleted AND status IN ('REQUESTED','ACTIVE');
