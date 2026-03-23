-- ============================================================
-- Tenant schema
-- ============================================================

-- groups (created before agents due to FK dependency)
CREATE TABLE IF NOT EXISTS groups (
    id         VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    name       VARCHAR(100) NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_groups_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
CREATE INDEX IF NOT EXISTS idx_groups_active ON groups (id) WHERE deleted = FALSE;

-- agents
CREATE TABLE IF NOT EXISTS agents (
    id            VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(256),
    role          VARCHAR(20)  NOT NULL DEFAULT 'COUNSELOR',
    group_id      VARCHAR(36)  REFERENCES groups(id),
    agent_status  VARCHAR(20)  NOT NULL DEFAULT 'OFFLINE',
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_agents_username UNIQUE (username),
    CONSTRAINT ck_agents_role CHECK (role IN ('ADMIN', 'COUNSELOR')),
    CONSTRAINT ck_agents_status CHECK (agent_status IN ('ONLINE', 'OFFLINE', 'BUSY', 'AWAY', 'WRAP_UP'))
);
CREATE INDEX IF NOT EXISTS idx_agents_username_active ON agents (username) WHERE deleted = FALSE;

-- companies
CREATE TABLE IF NOT EXISTS companies (
    id         VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    name       VARCHAR(200) NOT NULL,
    contact    VARCHAR(100),
    address    TEXT,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- channels
CREATE TABLE IF NOT EXISTS channels (
    id               VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    agent_id         VARCHAR(36)  REFERENCES agents(id),
    status           VARCHAR(20)  NOT NULL DEFAULT 'WAITING',
    started_at       TIMESTAMPTZ,
    ended_at         TIMESTAMPTZ,
    recording_path   TEXT,
    livekit_room_name VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_channels_status CHECK (status IN ('WAITING', 'IN_PROGRESS', 'CLOSED'))
);
CREATE INDEX IF NOT EXISTS idx_channels_agent_active ON channels (agent_id) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_channels_status_active ON channels (status) WHERE deleted = FALSE;

-- endpoints
CREATE TABLE IF NOT EXISTS endpoints (
    id               VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    channel_id       VARCHAR(36)  NOT NULL REFERENCES channels(id),
    type             VARCHAR(20)  NOT NULL,
    customer_name    VARCHAR(100),
    customer_contact VARCHAR(100),
    joined_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    left_at          TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_endpoints_channel ON endpoints (channel_id);

-- chat_messages
CREATE TABLE IF NOT EXISTS chat_messages (
    id          VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    channel_id  VARCHAR(36)  NOT NULL REFERENCES channels(id),
    sender_type VARCHAR(20)  NOT NULL,
    sender_id   VARCHAR(100) NOT NULL,
    content     TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_chat_messages_channel ON chat_messages (channel_id, created_at);

-- counsel_notes
CREATE TABLE IF NOT EXISTS counsel_notes (
    id         VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    channel_id VARCHAR(36)  NOT NULL REFERENCES channels(id),
    agent_id   VARCHAR(36)  NOT NULL REFERENCES agents(id),
    content    TEXT         NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted    BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_counsel_notes_channel_active ON counsel_notes (channel_id) WHERE deleted = FALSE;

-- feedbacks
CREATE TABLE IF NOT EXISTS feedbacks (
    id         VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    channel_id VARCHAR(36)  NOT NULL UNIQUE,
    rating     INT          NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment    TEXT,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- recordings
CREATE TABLE IF NOT EXISTS recordings (
    id          VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    channel_id  VARCHAR(36)  NOT NULL REFERENCES channels(id),
    egress_id   VARCHAR(100) NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'RECORDING',
    file_path   TEXT,
    started_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    stopped_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted     BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_recordings_status CHECK (status IN ('RECORDING', 'STOPPED', 'FAILED'))
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_recordings_active ON recordings (channel_id) WHERE deleted = FALSE AND status = 'RECORDING';

-- notifications
CREATE TABLE IF NOT EXISTS notifications (
    id              VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    recipient_id    VARCHAR(36)  NOT NULL,
    recipient_type  VARCHAR(20)  NOT NULL,
    type            VARCHAR(40)  NOT NULL,
    title           VARCHAR(200) NOT NULL,
    body            TEXT         NOT NULL,
    reference_id    VARCHAR(36),
    reference_type  VARCHAR(50),
    delivery_method VARCHAR(20)  NOT NULL DEFAULT 'IN_APP',
    read            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON notifications (recipient_id, read, created_at DESC) WHERE deleted = FALSE;

-- shared_files
CREATE TABLE IF NOT EXISTS shared_files (
    id                VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    channel_id        VARCHAR(36)  NOT NULL REFERENCES channels(id),
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
CREATE INDEX IF NOT EXISTS idx_shared_files_channel ON shared_files (channel_id) WHERE deleted = FALSE;

-- screen_captures
CREATE TABLE IF NOT EXISTS screen_captures (
    id                VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    channel_id        VARCHAR(36)  NOT NULL REFERENCES channels(id),
    captured_by       VARCHAR(36)  NOT NULL REFERENCES agents(id),
    original_filename VARCHAR(255) NOT NULL,
    stored_filename   VARCHAR(255) NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    file_size         BIGINT       NOT NULL,
    storage_path      TEXT         NOT NULL,
    note              TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted           BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_screen_captures_channel ON screen_captures (channel_id) WHERE deleted = FALSE;

-- co_browsing_sessions
CREATE TABLE IF NOT EXISTS co_browsing_sessions (
    id           VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::TEXT,
    channel_id   VARCHAR(36)  NOT NULL REFERENCES channels(id),
    initiated_by VARCHAR(36)  NOT NULL REFERENCES agents(id),
    status       VARCHAR(20)  NOT NULL DEFAULT 'REQUESTED',
    started_at   TIMESTAMPTZ,
    ended_at     TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_co_browsing_status CHECK (status IN ('REQUESTED', 'ACTIVE', 'ENDED'))
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_co_browsing_active ON co_browsing_sessions (channel_id) WHERE NOT deleted AND status IN ('REQUESTED', 'ACTIVE');
