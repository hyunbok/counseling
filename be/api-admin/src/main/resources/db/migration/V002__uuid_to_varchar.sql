-- Convert UUID primary keys to VARCHAR(36) in meta schema
-- This allows Spring Data R2DBC to use String IDs with null-check for INSERT vs UPDATE

-- tenants table
ALTER TABLE tenants
    ALTER COLUMN id TYPE VARCHAR(36) USING id::TEXT,
    ALTER COLUMN id SET DEFAULT gen_random_uuid()::TEXT;

-- super_admins table
ALTER TABLE super_admins
    ALTER COLUMN id TYPE VARCHAR(36) USING id::TEXT,
    ALTER COLUMN id SET DEFAULT gen_random_uuid()::TEXT;
