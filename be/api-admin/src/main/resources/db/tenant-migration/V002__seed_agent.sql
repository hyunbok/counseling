-- Seed default agent account
-- username: agent1 / password: agent123!
-- BCrypt hash generated with cost factor 10
INSERT INTO agents (id, username, password_hash, name, role, agent_status)
VALUES (
    gen_random_uuid(),
    'agent1',
    '$2a$10$N6luRc7wrIYCXa3C6QOoFuJ7QQlneqejPXWA6s4MNXLq1mQOMBMQe',
    '상담원1',
    'COUNSELOR',
    'OFFLINE'
)
ON CONFLICT (username) DO NOTHING;
