-- Seed super admin account
-- username: admin / password: admin123
INSERT INTO super_admins (id, username, password_hash, created_at, updated_at, deleted)
VALUES (
    gen_random_uuid()::TEXT,
    'admin',
    '$2a$10$wm/UN1Uvjd1DqWILXYIQQeSNHVsGy2ZU.zmVhS.TplaxSg1kggX7u',
    NOW(), NOW(), FALSE
)
ON CONFLICT (username) DO NOTHING;

-- Seed default tenant
INSERT INTO tenants (id, name, slug, status, db_host, db_port, db_name, db_username, db_password, created_at, updated_at, deleted)
VALUES (
    gen_random_uuid()::TEXT,
    'Default Company',
    'default',
    'ACTIVE',
    'localhost', 5433, 'tenant_default', 'admin', 'admin',
    NOW(), NOW(), FALSE
)
ON CONFLICT (slug) DO NOTHING;
