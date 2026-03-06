-- ============================================================
-- Tenant initial schema
-- ============================================================

-- agents
CREATE TABLE agents (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(256),
    role          VARCHAR(20)  NOT NULL DEFAULT 'COUNSELOR',
    group_id      UUID,
    agent_status  VARCHAR(20)  NOT NULL DEFAULT 'OFFLINE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_agents_username UNIQUE (username),
    CONSTRAINT ck_agents_role CHECK (role IN ('ADMIN', 'COUNSELOR')),
    CONSTRAINT ck_agents_status CHECK (agent_status IN ('ONLINE', 'OFFLINE', 'BUSY', 'AWAY', 'WRAP_UP'))
);
CREATE INDEX idx_agents_username_active ON agents (username) WHERE deleted = FALSE;

-- groups
CREATE TABLE groups (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted    BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_groups_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
CREATE INDEX idx_groups_active ON groups (id) WHERE deleted = FALSE;

ALTER TABLE agents ADD CONSTRAINT fk_agents_group FOREIGN KEY (group_id) REFERENCES groups(id);

-- companies
CREATE TABLE companies (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(200) NOT NULL,
    contact    VARCHAR(100),
    address    TEXT,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- channels
CREATE TABLE channels (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id          UUID        REFERENCES agents(id),
    status            VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    livekit_room_name VARCHAR(200),
    started_at        TIMESTAMPTZ,
    ended_at          TIMESTAMPTZ,
    recording_path    TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted           BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_channels_status CHECK (status IN ('WAITING', 'IN_PROGRESS', 'CLOSED'))
);
CREATE INDEX idx_channels_agent_active ON channels (agent_id) WHERE deleted = FALSE;
CREATE INDEX idx_channels_status_active ON channels (status) WHERE deleted = FALSE;
CREATE INDEX idx_channels_livekit_room ON channels (livekit_room_name) WHERE livekit_room_name IS NOT NULL;

-- endpoints
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

-- chat_messages
CREATE TABLE chat_messages (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id  UUID         NOT NULL REFERENCES channels(id),
    sender_type VARCHAR(20)  NOT NULL,
    sender_id   VARCHAR(100) NOT NULL,
    content     TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_chat_messages_sender_type CHECK (sender_type IN ('AGENT', 'CUSTOMER'))
);
CREATE INDEX idx_chat_messages_channel ON chat_messages (channel_id, created_at);

-- counsel_notes
CREATE TABLE counsel_notes (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id UUID        NOT NULL REFERENCES channels(id),
    agent_id   UUID        NOT NULL REFERENCES agents(id),
    content    TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted    BOOLEAN     NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_counsel_notes_channel_active ON counsel_notes (channel_id) WHERE deleted = FALSE;

-- feedbacks
CREATE TABLE feedbacks (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id UUID        NOT NULL UNIQUE REFERENCES channels(id),
    rating     SMALLINT    NOT NULL,
    comment    TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_feedbacks_rating CHECK (rating BETWEEN 1 AND 5)
);

-- recordings
CREATE TABLE recordings (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id  UUID         NOT NULL REFERENCES channels(id),
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
CREATE INDEX idx_recordings_channel ON recordings (channel_id) WHERE deleted = FALSE;
CREATE UNIQUE INDEX idx_recordings_active ON recordings (channel_id) WHERE deleted = FALSE AND status = 'RECORDING';

-- notifications
CREATE TABLE notifications (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id    UUID         NOT NULL,
    recipient_type  VARCHAR(20)  NOT NULL,
    type            VARCHAR(40)  NOT NULL,
    title           VARCHAR(200) NOT NULL,
    body            TEXT         NOT NULL,
    reference_id    UUID,
    reference_type  VARCHAR(30),
    delivery_method VARCHAR(10)  NOT NULL DEFAULT 'IN_APP',
    read            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_notifications_recipient_unread ON notifications (recipient_id, created_at DESC) WHERE deleted = FALSE AND read = FALSE;
CREATE INDEX idx_notifications_recipient_all ON notifications (recipient_id, created_at DESC) WHERE deleted = FALSE;

-- shared_files
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

-- screen_captures
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

-- co_browsing_sessions
CREATE TABLE co_browsing_sessions (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id   UUID        NOT NULL REFERENCES channels(id),
    initiated_by UUID        NOT NULL REFERENCES agents(id),
    status       VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    started_at   TIMESTAMPTZ,
    ended_at     TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted      BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_co_browsing_status CHECK (status IN ('REQUESTED', 'ACTIVE', 'ENDED'))
);
CREATE INDEX idx_co_browsing_channel ON co_browsing_sessions (channel_id, created_at DESC) WHERE NOT deleted;
CREATE UNIQUE INDEX idx_co_browsing_active ON co_browsing_sessions (channel_id) WHERE NOT deleted AND status IN ('REQUESTED', 'ACTIVE');
