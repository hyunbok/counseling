# v1-11: Implement Admin API Endpoints - Design Plan

## Overview
Admin API for `be/api-admin` (port 8081, prefix `/api-adm`).
Kotlin 2.3 + Spring Boot 4.0.2 + WebFlux + R2DBC + MongoDB + Redis. Hexagonal + CQRS.

## Key Decisions
1. **Domain models duplicated** in api-admin (separate Gradle projects, no shared lib)
2. **AdminRole**: `SUPER_ADMIN`, `COMPANY_ADMIN`, `GROUP_ADMIN` (different from be/api's ADMIN/COUNSELOR)
3. **JWT**: Separate `AdminJjwtTokenProvider`; `tenantId` claim nullable for SuperAdmin
4. **DB access**: meta_db for tenants/super_admins; tenant-routing for agents/groups/companies
5. **Stats**: Pre-aggregated MongoDB read models (event listeners in be/api populate them)
6. **Real-time monitoring**: Redis for agent status, live PostgreSQL query for active channels
7. **GROUP_ADMIN filtering**: Service-layer enforced (same SecurityConfig paths as COMPANY_ADMIN)

## API Endpoints

### Auth (`/api-adm/auth`) - Public
| Method | Path | Description |
|--------|------|-------------|
| POST | /auth/login | Login (type: SUPER_ADMIN or TENANT_ADMIN) |
| POST | /auth/logout | Blacklist token (authenticated) |
| POST | /auth/refresh | Refresh token pair |

### Tenants (`/api-adm/tenants`) - SUPER_ADMIN only
| Method | Path | Description |
|--------|------|-------------|
| GET | /tenants | List (pagination, filter by status/search) |
| POST | /tenants | Create tenant |
| PUT | /tenants/{id} | Update tenant |
| PATCH | /tenants/{id}/status | Toggle status (ACTIVE/SUSPENDED/DEACTIVATED) |

### Groups (`/api-adm/groups`) - COMPANY_ADMIN+
| Method | Path | Description |
|--------|------|-------------|
| GET | /groups | List groups (GROUP_ADMIN sees own only) |
| POST | /groups | Create group (COMPANY_ADMIN+) |
| PUT | /groups/{id} | Update group |
| DELETE | /groups/{id} | Soft delete (COMPANY_ADMIN+) |

### Company (`/api-adm/company`) - COMPANY_ADMIN+
| Method | Path | Description |
|--------|------|-------------|
| GET | /company | Get company info |
| PUT | /company | Update company info |

### Agents (`/api-adm/agents`) - COMPANY_ADMIN+ (GROUP_ADMIN filtered)
| Method | Path | Description |
|--------|------|-------------|
| GET | /agents | List (filters: groupId, active, search) |
| POST | /agents | Create agent (generates temp password) |
| PUT | /agents/{id} | Update agent |
| PATCH | /agents/{id}/status | Activate/deactivate |
| POST | /agents/{id}/reset-password | Generate temp password |

### Stats (`/api-adm/stats`) - All admin roles
| Method | Path | Description |
|--------|------|-------------|
| GET | /stats/summary | Date-range summary (MongoDB) |
| GET | /stats/agents | Per-agent stats (MongoDB) |

### Monitoring (`/api-adm/monitoring`) - All admin roles
| Method | Path | Description |
|--------|------|-------------|
| GET | /monitoring/channels | Active channels (PostgreSQL live) |
| GET | /monitoring/agents | Agent statuses (Redis + PostgreSQL) |

### Feedback (`/api-adm/feedbacks`) - All admin roles
| Method | Path | Description |
|--------|------|-------------|
| GET | /feedbacks | List (filters: rating, agentId, date range) |
| GET | /feedbacks/{id} | Detail |

## SecurityConfig Update
```
/api-adm/auth/login, /auth/refresh  -> permitAll()
/actuator/health, /actuator/info    -> permitAll()
/api-adm/tenants/**                 -> ROLE_SUPER_ADMIN
/api-adm/company/**                 -> SUPER_ADMIN | COMPANY_ADMIN
/api-adm/groups/**, /agents/**, /stats/**, /monitoring/**, /feedbacks/** -> all admin roles
/api-adm/**                         -> authenticated()
anyExchange()                       -> denyAll()
```

## Package Structure (`com.counseling.admin/`)
```
domain/          AdminRole, Agent, Company, Group, Tenant, SuperAdmin, Channel,
                 Feedback, TenantContext, auth/*, exception/*
port/inbound/    AdminAuthUseCase, TenantManagementUseCase, GroupManagementUseCase,
                 CompanyManagementUseCase, AgentManagementUseCase, StatsQuery,
                 MonitoringQuery, FeedbackQuery
port/outbound/   Admin*Repository (Tenant, SuperAdmin, Agent, Group, Company,
                 Channel, Endpoint), AdminJwtTokenProvider, TokenBlacklistRepository,
                 TenantConnectionRegistry, StatsReadRepository, FeedbackReadRepository,
                 AgentStatusCacheRepository
application/     AdminAuthService, TenantManagementService, GroupManagementService,
                 CompanyManagementService, AgentManagementService, StatsQueryService,
                 MonitoringQueryService, FeedbackQueryService, PasswordGenerator
adapter/inbound/ controllers (8), dto files (10), filters (2), GlobalExceptionHandler
adapter/outbound/ R2DBC repos (7), MongoDB repos (2), Redis repos (2), JjwtProvider,
                  TenantRouting, documents (3)
config/          R2dbcConfig, JwtProperties, TenantBootstrap, RedisConfig, SecurityConfig(mod)
```

## Build Sequence (Implementation Phases)

### Phase 1: Shared Infrastructure (subtask 0)
Domain enums, auth models, exceptions, TenantContext, JWT provider, Redis blacklist,
tenant routing, filters, GlobalExceptionHandler, configs (R2dbc, JWT, Redis, TenantBootstrap),
SecurityConfig update, PageResponse/ErrorResponse DTOs.

### Phase 2: Admin Auth (subtask 5, auth part)
SuperAdmin domain, AdminAccountRepository, AdminAuthUseCase, AdminAuthService,
AdminAuthController, AuthDtos.

### Phase 3: Tenant Management (subtask 1)
Tenant domain, TenantRepository, TenantManagementUseCase/Service, TenantController, TenantDtos.

### Phase 4: Group & Company (subtask 2)
Company/Group domains, repositories, use cases, services, controllers, DTOs.

### Phase 5: Agent Management (subtask 3)
Agent domain, repository, use case, service (BCrypt + PasswordGenerator), controller, DTOs.

### Phase 6: Stats & Monitoring (subtask 4)
Channel domain, StatsReadRepository, AgentStatusCacheRepository, MongoDB/Redis adapters,
StatsQueryService, MonitoringQueryService, controllers, DTOs.

### Phase 7: Feedback (subtask 5, feedback part)
Feedback domain, FeedbackReadRepository, MongoDB adapter, FeedbackQueryService, controller, DTOs.

## Data Model Notes
- **meta_db**: `tenants`, `super_admins` tables (existing, reused)
- **tenant DBs**: `agents`, `groups`, `companies`, `channels`, `endpoints`, `feedbacks` (existing)
- **MongoDB**: `admin_channel_stats`, `admin_agent_stats`, `admin_feedbacks` collections (new)
- **Redis**: `auth:blacklist:<jti>`, `agent:status:<tenant>:<agentId>` keys

## ~65 production files + corresponding test files
