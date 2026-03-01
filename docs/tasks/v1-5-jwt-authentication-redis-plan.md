# v1-5: JWT Authentication with Redis — Design Document

## 1. Overview

JWT-based authentication for tenant-scoped agents (counselors/admins) with access/refresh token pairs, Redis blacklisting, and Spring Security WebFlux integration.

**CQRS side:** Command-side only — auth mutates state (token issuance, blacklisting, password change). No query-side read model needed.

**Auth target:** Agents in tenant databases (not SuperAdmins in meta DB). Each `/api/auth/**` endpoint requires `X-Tenant-Id` header (existing `TenantWebFilter` handles this).

**Dependencies already present:** Spring Security, Redis Reactive, JJWT 0.12.6 — no `build.gradle.kts` changes needed.

## 2. API Design

All endpoints require `X-Tenant-Id` header. Token format: `Authorization: Bearer <jwt>`.

| Method | Path | Auth | Request | Response |
|--------|------|------|---------|----------|
| POST | `/api/auth/login` | No | `{username, password}` | `200 {accessToken, refreshToken, expiresIn, agent}` |
| POST | `/api/auth/logout` | Yes | — | `204 No Content` |
| POST | `/api/auth/refresh` | No | `{refreshToken}` | `200 {accessToken, refreshToken, expiresIn}` |
| PUT | `/api/auth/password` | Yes | `{currentPassword, newPassword}` | `204 No Content` |

Error responses: `401` invalid credentials/token, `400` bad request, `403` cross-tenant access.

## 3. JWT Token Design

**Access Token** (1h): `{sub: agentId, tid: tenantSlug, role: COUNSELOR, type: ACCESS, jti: uuid, iat, exp}`
**Refresh Token** (7d): `{sub: agentId, tid: tenantSlug, type: REFRESH, jti: uuid, iat, exp}`

Signed with HMAC-SHA256 via `jwt.secret`. JTI used as Redis blacklist key.
Refresh token rotation: old refresh token blacklisted on each refresh.

## 4. Redis Blacklist

- Key: `auth:blacklist:{jti}`, Value: `"1"`, TTL: remaining token lifetime
- Auto-cleanup via Redis TTL expiry

## 5. Data Model

### agents table (tenant DB — NOT meta DB)

```sql
CREATE TABLE agents (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'COUNSELOR',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_agents_username UNIQUE (username),
    CONSTRAINT ck_agents_role CHECK (role IN ('ADMIN', 'COUNSELOR'))
);
CREATE INDEX idx_agents_username_active ON agents (username) WHERE deleted = FALSE;
```

## 6. Security Filter Chain

```
Request → TenantWebFilter (HIGHEST_PRECEDENCE) → JwtAuthenticationWebFilter → SecurityConfig rules → Controller
```

- `JwtAuthenticationWebFilter`: extracts Bearer token, validates signature+expiry, checks Redis blacklist, verifies JWT `tid` matches `X-Tenant-Id` header, sets `SecurityContext`
- **NOT a `@Component`** — registered manually via `SecurityConfig.addFilterAt()` to avoid double registration

Public paths: `/api/auth/login`, `/api/auth/refresh`, `/api/super-admin/**`, `/actuator/**`
Authenticated: all other `/api/**`

## 7. Implementation File List

Base: `be/api/src/main/kotlin/com/counseling/api/`

### Subtask 1: Domain Models + Config

| File | Role |
|------|------|
| `domain/auth/TokenType.kt` | Enum: ACCESS, REFRESH |
| `domain/auth/TokenPair.kt` | Data class: accessToken, refreshToken, expiresIn |
| `domain/auth/JwtClaims.kt` | Parsed JWT payload (sub, role, tenantId, tokenType, jti, iat, exp) |
| `domain/Agent.kt` | Agent domain model (id, username, passwordHash, name, role) |
| `domain/AgentRole.kt` | Enum: ADMIN, COUNSELOR |
| `domain/auth/AuthenticatedAgent.kt` | Value object for SecurityContext |
| `domain/exception/UnauthorizedException.kt` | 401 exception |
| `domain/exception/BadRequestException.kt` | 400 exception |
| `config/JwtProperties.kt` | `@ConfigurationProperties(prefix = "jwt")` |
| `ApiApplication.kt` (MODIFY) | Add `@ConfigurationPropertiesScan` |

### Subtask 2: JwtTokenService (JJWT)

| File | Role |
|------|------|
| `port/outbound/JwtTokenProvider.kt` | Interface: generateTokenPair, parseAccessToken, parseRefreshToken |
| `adapter/outbound/external/JjwtTokenProvider.kt` | JJWT 0.12.6 implementation |

### Subtask 3: Redis Blacklist

| File | Role |
|------|------|
| `port/outbound/TokenBlacklistRepository.kt` | Interface: blacklist(jti, ttl), isBlacklisted(jti) |
| `adapter/outbound/external/RedisTokenBlacklistRepository.kt` | ReactiveStringRedisTemplate impl |
| `config/RedisConfig.kt` | ReactiveStringRedisTemplate bean (`@Profile("!test")`) |

### Subtask 4: Auth Endpoints

| File | Role |
|------|------|
| `port/inbound/AuthUseCase.kt` | Interface: login, logout, refresh, changePassword |
| `port/outbound/AgentRepository.kt` | Interface: findByUsername, findById, save |
| `application/AuthService.kt` | AuthUseCase implementation |
| `adapter/outbound/persistence/AgentR2dbcRepository.kt` | Tenant-routed DatabaseClient SQL |
| `adapter/inbound/web/dto/AuthDtos.kt` | Request/Response DTOs |
| `adapter/inbound/web/dto/ErrorResponse.kt` | Error DTOs |
| `adapter/inbound/web/controller/AuthController.kt` | REST controller at `/api/auth` |
| `adapter/inbound/web/GlobalExceptionHandler.kt` | `@RestControllerAdvice` |

### Subtask 5: Security Config + JWT Filter

| File | Role |
|------|------|
| `adapter/inbound/web/filter/JwtAuthenticationWebFilter.kt` | Bearer extraction, validation, blacklist check, cross-tenant guard |
| `config/SecurityConfig.kt` (MODIFY) | Add JWT filter, path rules, BCryptPasswordEncoder bean |

### DB Migration

| File | Role |
|------|------|
| `src/main/resources/db/tenant-migration/V001__create_agents.sql` | Agents table for tenant DBs |

### Test Files

| File | Role |
|------|------|
| `test/.../domain/auth/JwtClaimsTest.kt` | Domain model tests |
| `test/.../adapter/outbound/external/JjwtTokenProviderTest.kt` | Token gen/parse/validate tests |
| `test/.../application/AuthServiceTest.kt` | Use case tests (MockK) |
| `test/.../adapter/inbound/web/controller/AuthControllerTest.kt` | Endpoint tests |
| `test/.../adapter/inbound/web/filter/JwtAuthenticationWebFilterTest.kt` | Filter tests |
| `test/resources/application-test.yml` (MODIFY) | Add jwt.* test properties |

## 8. Key Decisions

1. **Agent (tenant-scoped)**, not SuperAdmin — auth targets tenant DBs via `TenantRoutingConnectionFactory`
2. **JTI-based blacklisting** — each token gets a UUID JTI, blacklist checks use JTI not full token string
3. **Cross-tenant guard** — JWT `tid` must match `X-Tenant-Id` header (enforced in filter)
4. **`@ConfigurationProperties`** introduced for JWT config (new pattern for this codebase)
5. **`AgentR2dbcRepository`** uses tenant-routed `DatabaseClient` (not `metaDatabaseClient`)
6. **Refresh rotation** — old refresh token blacklisted on each refresh call

## 9. Build Sequence

**Phase 1 — Domain + Config** (Subtask 1)
Create domain models, enums, exceptions, JwtProperties, DB migration. Verify: `./gradlew compileKotlin`

**Phase 2 — Ports + Outbound Adapters** (Subtasks 2, 3)
Create port interfaces, JjwtTokenProvider, RedisTokenBlacklistRepository, RedisConfig. Verify: `./gradlew test`

**Phase 3 — Application Service + Inbound** (Subtask 4)
Create AuthUseCase, AgentRepository, AuthService, AgentR2dbcRepository, AuthController, DTOs, error handling. Verify: `./gradlew test`

**Phase 4 — Security Integration** (Subtask 5)
Create JwtAuthenticationWebFilter, update SecurityConfig. Verify: `./gradlew test` (including context load)
