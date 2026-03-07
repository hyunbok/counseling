-- Seed default agent account
-- username: agent1 / password: agent123!
-- BCrypt hash generated with cost factor 10
INSERT INTO agents (id, username, password_hash, name, role, agent_status)
VALUES (
    gen_random_uuid(),
    'agent1',
    '$2b$10$4lDW9ZAoQfpl4/IN7Sb6sujkmvMYEOBrSax8aH97ekHaPPvfXVJ8S',
    '상담원1',
    'COUNSELOR',
    'OFFLINE'
)
ON CONFLICT (username) DO NOTHING;
