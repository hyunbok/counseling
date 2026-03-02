ALTER TABLE channels ADD COLUMN livekit_room_name VARCHAR(200);
CREATE INDEX idx_channels_livekit_room ON channels (livekit_room_name) WHERE livekit_room_name IS NOT NULL;
