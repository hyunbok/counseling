# v1-4: Multi-Tenant Database Routing - Design Document

## 1. Overview

Database-per-tenant routing for the counseling platform. A **Meta DB** (PostgreSQL) stores tenant registry and super admin credentials. Each tenant gets its own PostgreSQL database. Routing is resolved per-request via Reactor Context (not ThreadLocal — this is WebFlux).

**CQRS Flow**: TenantWebFilter extracts tenant slug from `X-Tenant-Id` header → injects into Reactor Context → TenantRoutingConnectionFactory reads context → delegates to per-tenant ConnectionPool.

**Dependencies**: Task 2 (be/api setup) ✅, Task 3 (be/api-admin setup) ✅

## 2. API Design

Super admin endpoints (tenant management, served from be/api for now):

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/super-admin/tenants` | Create tenant |
| GET | `/api/super-admin/tenants` | List all tenants |
| GET | `/api/super-admin/tenants/{id}` | Get tenant by ID |
| PATCH | `/api/super-admin/tenants/{id}/activate` | Activate tenant |
| PATCH | `/api/super-admin/tenants/{id}/suspend` | Suspend tenant |
| DELETE | `/api/super-admin/tenants/{id}` | Soft delete tenant |

**Request DTOs**: `CreateTenantRequest(name, slug, dbHost, dbPort, dbName, dbUsername, dbPassword)`
**Response DTOs**: `TenantResponse(id, name, slug, status, dbHost, dbPort, dbName, createdAt, updatedAt)` — excludes credentials

## 3. Data Model

### Meta DB Schema (PostgreSQL — `V001__create_meta_schema.sql`)

```sql
CREATE TABLE tenants (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(100) NOT NULL,
    slug          VARCHAR(50)  NOT NULL UNIQUE,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    db_host       VARCHAR(255) NOT NULL,
    db_port       INT          NOT NULL DEFAULT 5432,
    db_name       VARCHAR(100) NOT NULL,
    db_username   VARCHAR(100) NOT NULL,
    db_password   VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_tenants_status ON tenants(status) WHERE deleted = FALSE;
CREATE INDEX idx_tenants_slug ON tenants(slug) WHERE deleted = FALSE;

CREATE TABLE super_admins (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_super_admins_username ON super_admins(username) WHERE deleted = FALSE;
```

### Domain Models
- `Tenant`: id, name, slug, status, dbHost/Port/Name/Username/Password, timestamps, deleted
- `TenantStatus`: enum `PENDING | ACTIVE | SUSPENDED | DEACTIVATED`
- `SuperAdmin`: id, username, passwordHash, timestamps, deleted
- `TenantContext`: Reactor Context-based tenant ID propagation (pure domain, no Spring deps)

## 4. Implementation File List

All paths under `be/api/src/main/kotlin/com/counseling/api/`:

### Subtask 1: Meta DB Schema & Entities
| File | Role |
|------|------|
| `src/main/resources/db/migration/V001__create_meta_schema.sql` | SQL migration |
| `domain/TenantStatus.kt` | Enum (PENDING, ACTIVE, SUSPENDED, DEACTIVATED) |
| `domain/Tenant.kt` | Domain model with state transition methods |
| `domain/SuperAdmin.kt` | Domain model |
| `port/outbound/TenantRepository.kt` | Outbound port interface |
| `adapter/outbound/persistence/TenantR2dbcRepository.kt` | R2DBC adapter using @Qualifier("metaDatabaseClient") |

### Subtask 2: TenantContext
| File | Role |
|------|------|
| `domain/TenantContext.kt` | Reactor Context read/write utilities |

### Subtask 3: TenantConnectionRegistry
| File | Role |
|------|------|
| `port/outbound/TenantConnectionRegistry.kt` | Port interface |
| `adapter/outbound/persistence/TenantConnectionRegistryImpl.kt` | ConcurrentHashMap-backed ConnectionPool cache |

### Subtask 4: TenantRoutingConnectionFactory & R2DBC Config
| File | Role |
|------|------|
| `adapter/outbound/persistence/TenantRoutingConnectionFactory.kt` | Custom ConnectionFactory (NOT AbstractRoutingConnectionFactory) |
| `config/R2dbcConfig.kt` | Meta CF + @Primary routing CF + DatabaseClient beans |
| `config/TenantBootstrap.kt` | ApplicationRunner: preload active tenant pools at startup |

### Subtask 5: TenantWebFilter
| File | Role |
|------|------|
| `adapter/inbound/web/filter/TenantWebFilter.kt` | WebFilter: extract X-Tenant-Id, inject into Reactor Context |

### Files to Modify
| File | Change |
|------|--------|
| `config/WebFluxConfig.kt` | Add `X-Tenant-Id` to CORS allowedHeaders |

### Test Files
| File | Scope |
|------|-------|
| `domain/TenantTest.kt` | Domain model state transitions |
| `domain/TenantContextTest.kt` | Reactor Context propagation |
| `adapter/outbound/persistence/TenantConnectionRegistryImplTest.kt` | Registry cache behavior |
| `adapter/inbound/web/filter/TenantWebFilterTest.kt` | Header extraction, context injection |
| `config/R2dbcConfigTest.kt` | Bean wiring verification |

## 5. Key Design Decisions

1. **Custom ConnectionFactory over AbstractRoutingConnectionFactory**: The abstract class requires static `setTargetConnectionFactories(Map)` at init time. Tenants are dynamic (created/removed at runtime), so we implement `ConnectionFactory` directly and delegate to registry.

2. **`slug` as routing key**: Human-readable, URL-safe (e.g., "acme-counseling"). Used in `X-Tenant-Id` header and as registry key. UUIDs are internal only.

3. **Two-tier DatabaseClient**: Meta repositories use `@Qualifier("metaDatabaseClient")`. All tenant-scoped repositories use `@Primary` routing factory by default. Prevents accidental cross-routing.

4. **Eager pool creation**: Pools created at tenant activation (not lazily). Avoids first-request latency. Issues detected at activation time.

5. **No new Gradle dependencies needed**: `AbstractRoutingConnectionFactory`, `ConnectionPool`, `r2dbc-postgresql` all already on classpath.

## 6. Build Sequence

### Phase 1 — Domain & Ports (Subtask 1+2, parallelizable)
- Domain models (Tenant, TenantStatus, SuperAdmin, TenantContext)
- Port interfaces (TenantRepository, TenantConnectionRegistry)
- Unit tests for domain models

### Phase 2 — Infrastructure (Subtask 3+4, depends on Phase 1)
- TenantConnectionRegistryImpl
- TenantRoutingConnectionFactory
- R2dbcConfig (meta CF + routing CF)
- TenantBootstrap
- TenantR2dbcRepository

### Phase 3 — Web Layer (Subtask 5, depends on Phase 2)
- TenantWebFilter
- WebFluxConfig update
- Integration tests

### Phase 4 — Verification
- Build passes, existing tests still pass
- SQL migration file present
