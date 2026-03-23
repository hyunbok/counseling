-- Seed default admin agent
-- username: agent1 / password: agent123 / role: ADMIN
INSERT INTO agents (id, username, password_hash, name, email, role, agent_status, active, created_at, updated_at, deleted)
VALUES (
    gen_random_uuid()::TEXT,
    'agent1',
    '$2a$10$g9nhW.uRwowYLjE.JD.KueKteIecTpYziYFfcziXNNQPtNYchUuNe',
    'Admin Agent',
    'agent1@example.com',
    'ADMIN', 'OFFLINE', TRUE,
    NOW(), NOW(), FALSE
)
ON CONFLICT (username) DO NOTHING;

-- Seed default counselor agent
-- username: counselor1 / password: counselor123 / role: COUNSELOR
INSERT INTO agents (id, username, password_hash, name, email, role, agent_status, active, created_at, updated_at, deleted)
VALUES (
    gen_random_uuid()::TEXT,
    'counselor1',
    '$2a$10$W3YSXmMW9xZVaxvvV9OSSu92FLQthCpPCrZiY4uVeKM/Fnas4CTLa',
    'Counselor Agent',
    'counselor1@example.com',
    'COUNSELOR', 'OFFLINE', TRUE,
    NOW(), NOW(), FALSE
)
ON CONFLICT (username) DO NOTHING;

-- Seed default company
INSERT INTO companies (id, name, contact, address, created_at, updated_at)
VALUES (
    gen_random_uuid()::TEXT,
    'Default Company',
    '02-1234-5678',
    'Seoul, South Korea',
    NOW(), NOW()
);

-- Seed default group
INSERT INTO groups (id, name, status, created_at, updated_at, deleted)
VALUES (
    gen_random_uuid()::TEXT,
    'General',
    'ACTIVE',
    NOW(), NOW(), FALSE
);
