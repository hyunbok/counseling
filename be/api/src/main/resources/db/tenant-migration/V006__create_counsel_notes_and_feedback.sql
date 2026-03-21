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

CREATE TABLE feedbacks (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id UUID        NOT NULL UNIQUE REFERENCES channels(id),
    rating     SMALLINT    NOT NULL,
    comment    TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_feedbacks_rating CHECK (rating BETWEEN 1 AND 5)
);
