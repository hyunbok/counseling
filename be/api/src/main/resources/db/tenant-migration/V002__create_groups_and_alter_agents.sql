CREATE TABLE groups (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_groups_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
CREATE INDEX idx_groups_active ON groups (id) WHERE deleted = FALSE;

ALTER TABLE agents
    ADD COLUMN group_id    UUID        REFERENCES groups(id),
    ADD COLUMN agent_status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE';

ALTER TABLE agents
    ADD CONSTRAINT ck_agents_status CHECK (agent_status IN ('ONLINE', 'OFFLINE', 'BUSY', 'AWAY', 'WRAP_UP'));
