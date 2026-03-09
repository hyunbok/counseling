-- Seed default super admin account
-- username: admin / password: admin123!
-- BCrypt hash generated with cost factor 10
INSERT INTO super_admins (id, username, password_hash)
VALUES (
    gen_random_uuid(),
    'admin',
    '$2a$10$RZZPGACRo4osnutIb02m1.HYKTVL3obUKOKpzBCKTEjXem1ztdWNO'
)
ON CONFLICT (username) DO NOTHING;
