CREATE TABLE agents (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'COUNSELOR',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_agents_username UNIQUE (username),
    CONSTRAINT ck_agents_role CHECK (role IN ('ADMIN', 'COUNSELOR'))
);
CREATE INDEX idx_agents_username_active ON agents (username) WHERE deleted = FALSE;
