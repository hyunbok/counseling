-- Convert UUID columns to VARCHAR(36) in tenant schema
-- This allows Spring Data R2DBC to use String IDs with null-check for INSERT vs UPDATE

-- groups table
ALTER TABLE groups
    ALTER COLUMN id TYPE VARCHAR(36) USING id::TEXT,
    ALTER COLUMN id SET DEFAULT gen_random_uuid()::TEXT;

-- agents table
ALTER TABLE agents
    ALTER COLUMN id TYPE VARCHAR(36) USING id::TEXT,
    ALTER COLUMN id SET DEFAULT gen_random_uuid()::TEXT,
    ALTER COLUMN group_id TYPE VARCHAR(36) USING group_id::TEXT;

-- channels table
ALTER TABLE channels
    ALTER COLUMN id TYPE VARCHAR(36) USING id::TEXT,
    ALTER COLUMN id SET DEFAULT gen_random_uuid()::TEXT,
    ALTER COLUMN agent_id TYPE VARCHAR(36) USING agent_id::TEXT;

-- endpoints table
ALTER TABLE endpoints
    ALTER COLUMN id TYPE VARCHAR(36) USING id::TEXT,
    ALTER COLUMN id SET DEFAULT gen_random_uuid()::TEXT,
    ALTER COLUMN channel_id TYPE VARCHAR(36) USING channel_id::TEXT;

-- chat_messages table
ALTER TABLE chat_messages
    ALTER COLUMN id TYPE VARCHAR(36) USING id::TEXT,
    ALTER COLUMN id SET DEFAULT gen_random_uuid()::TEXT,
    ALTER COLUMN channel_id TYPE VARCHAR(36) USING channel_id::TEXT;

-- counsel_notes table
ALTER TABLE counsel_notes
    ALTER COLUMN id TYPE VARCHAR(36) USING id::TEXT,
    ALTER COLUMN id SET DEFAULT gen_random_uuid()::TEXT,
    ALTER COLUMN channel_id TYPE VARCHAR(36) USING channel_id::TEXT,
    ALTER COLUMN agent_id TYPE VARCHAR(36) USING agent_id::TEXT;

-- feedbacks table
ALTER TABLE feedbacks
    ALTER COLUMN id TYPE VARCHAR(36) USING id::TEXT,
    ALTER COLUMN id SET DEFAULT gen_random_uuid()::TEXT,
    ALTER COLUMN channel_id TYPE VARCHAR(36) USING channel_id::TEXT;

-- recordings table
ALTER TABLE recordings
    ALTER COLUMN id TYPE VARCHAR(36) USING id::TEXT,
    ALTER COLUMN id SET DEFAULT gen_random_uuid()::TEXT,
    ALTER COLUMN channel_id TYPE VARCHAR(36) USING channel_id::TEXT;

-- notifications table
ALTER TABLE notifications
    ALTER COLUMN id TYPE VARCHAR(36) USING id::TEXT,
    ALTER COLUMN id SET DEFAULT gen_random_uuid()::TEXT,
    ALTER COLUMN recipient_id TYPE VARCHAR(36) USING recipient_id::TEXT,
    ALTER COLUMN reference_id TYPE VARCHAR(36) USING reference_id::TEXT;

-- shared_files table
ALTER TABLE shared_files
    ALTER COLUMN id TYPE VARCHAR(36) USING id::TEXT,
    ALTER COLUMN id SET DEFAULT gen_random_uuid()::TEXT,
    ALTER COLUMN channel_id TYPE VARCHAR(36) USING channel_id::TEXT;

-- screen_captures table
ALTER TABLE screen_captures
    ALTER COLUMN id TYPE VARCHAR(36) USING id::TEXT,
    ALTER COLUMN id SET DEFAULT gen_random_uuid()::TEXT,
    ALTER COLUMN channel_id TYPE VARCHAR(36) USING channel_id::TEXT,
    ALTER COLUMN captured_by TYPE VARCHAR(36) USING captured_by::TEXT;

-- co_browsing_sessions table
ALTER TABLE co_browsing_sessions
    ALTER COLUMN id TYPE VARCHAR(36) USING id::TEXT,
    ALTER COLUMN id SET DEFAULT gen_random_uuid()::TEXT,
    ALTER COLUMN channel_id TYPE VARCHAR(36) USING channel_id::TEXT,
    ALTER COLUMN initiated_by TYPE VARCHAR(36) USING initiated_by::TEXT;
