# v1-7: Customer Queue Management — Design Document

## 1. Overview

Redis Sorted Set 기반 고객 대기열 시스템. 고객이 대기열에 진입하고, 상담사가 수락하면 Channel+Endpoint를 생성하여 상담을 시작한다.
**CQRS side:** Command-side. Queue는 Redis에 임시 저장(PostgreSQL 미사용), 수락 시 Channel/Endpoint를 PostgreSQL에 생성.
**Dependencies:** v1-5 (JWT Auth), v1-6 (Domain Models — Channel, Endpoint, Agent)

## 2. API Design

All endpoints require `X-Tenant-Id` header. Queue-scoped paths: `/api/queue/**`.

| Method | Path | Auth | Request | Response | Status |
|--------|------|------|---------|----------|--------|
| POST | `/api/queue/enter` | No | `{name, contact, groupId?}` | `{entryId, position, queueSize}` | 201 |
| DELETE | `/api/queue/{entryId}` | No | — | — | 204 |
| GET | `/api/queue` | Yes | — | `[{entryId, name, contact, groupId, enteredAt, waitDurationSeconds, position}]` | 200 |
| POST | `/api/queue/{entryId}/accept` | Yes | — | `{channelId, customerName, customerContact}` | 200 |
| GET | `/api/queue/position/{entryId}` | No | — | `{entryId, position, queueSize}` | 200 |
| GET | `/api/queue/stream` | Yes | — | SSE `Flux<QueueUpdateEvent>` | 200 |
| GET | `/api/queue/position/{entryId}/stream` | No | — | SSE `Flux<PositionUpdateEvent>` | 200 |

Errors: `400` bad request, `401` unauthenticated, `404` entry not found, `409` agent not available.

## 3. Domain Models

All in `com.counseling.api.domain`. QueueEntry is ephemeral (Redis only, not PostgreSQL).

| File | Type | Key Fields |
|------|------|------------|
| `QueueEntry.kt` | data class | id: UUID, customerName, customerContact, groupId?, enteredAt: Instant |
| `QueueUpdateType.kt` | enum | ENTERED, LEFT, ACCEPTED, QUEUE_CHANGED |
| `QueueUpdate.kt` | data class | type, entry?, queueSize, timestamp |
| `PositionUpdate.kt` | data class | entryId, position, queueSize, timestamp |
| `exception/ConflictException.kt` | exception | 409 |

## 4. Port Interfaces

| File | Role | Methods |
|------|------|---------|
| `port/inbound/QueueUseCase.kt` | Inbound | enterQueue, leaveQueue, acceptCustomer, getQueue, getPosition, subscribeQueueUpdates, subscribePositionUpdates. Companion DTOs: EnterQueueResult, AcceptResult, PositionResult, QueueEntryWithPosition |
| `port/outbound/QueueRepository.kt` | Redis ops | add, remove, findAll, findById, getPosition, getSize, removeAtomically |
| `port/outbound/QueueNotificationPort.kt` | SSE sink | emitQueueUpdate, emitPositionUpdate, subscribeAgentUpdates, subscribePositionUpdates |

## 5. Redis Key Design

- **Sorted Set:** `queue:{tenantId}` — score = enteredAt.toEpochMilli(), member = JSON(QueueEntry)
- **Hash:** `queue-idx:{tenantId}` — field = entryId, value = JSON(QueueEntry) (for O(1) member lookup → ZRANK)
- **Lua script** for atomic accept: HGET → ZREM → HDEL (prevents double-accept race condition)

## 6. SSE Notification Design

- Agent stream: `Sinks.Many<QueueUpdate>` per tenant, `multicast().onBackpressureBuffer()`
- Customer stream: `Sinks.Many<PositionUpdate>` per tenant, filtered by entryId in controller
- Heartbeat: `:heartbeat` comment every 30s
- Terminal signal: `position=0` means entry removed/accepted → stream completes

## 7. Implementation File List

### Subtask 1 — Domain Models + Ports + DTOs

| File | Role |
|------|------|
| `domain/QueueEntry.kt` | Queue entry domain model |
| `domain/QueueUpdateType.kt` | Enum for update types |
| `domain/QueueUpdate.kt` | Agent-facing SSE event model |
| `domain/PositionUpdate.kt` | Customer-facing SSE event model |
| `domain/exception/ConflictException.kt` | 409 exception |
| `port/inbound/QueueUseCase.kt` | Inbound port + result DTOs |
| `port/outbound/QueueRepository.kt` | Redis queue port |
| `port/outbound/QueueNotificationPort.kt` | SSE notification port |
| `adapter/inbound/web/dto/QueueDtos.kt` | Request/response DTOs |

### Subtask 2 — Redis Repository + Notification Adapter

| File | Role |
|------|------|
| `adapter/outbound/external/RedisQueueRepository.kt` | Redis sorted set + hash impl with Lua script |
| `adapter/outbound/external/InMemoryQueueNotificationAdapter.kt` | Sinks.Many per tenant |

### Subtask 3 — QueueService (accept flow creates Channel+Endpoints)

| File | Role |
|------|------|
| `application/QueueService.kt` | QueueUseCase impl: enterQueue, leaveQueue, acceptCustomer, SSE subscriptions |

### Subtask 4 — Controller + Security Integration

| File | Role |
|------|------|
| `adapter/inbound/web/controller/QueueController.kt` | REST + SSE endpoints |
| `config/SecurityConfig.kt` (MODIFY) | Add public paths: `/api/queue/enter`, `/api/queue/position/**`, DELETE `/api/queue/*` |
| `adapter/inbound/web/filter/JwtAuthenticationWebFilter.kt` (MODIFY) | Pass-through when no Bearer token (let SecurityConfig decide), add queue public prefixes |
| `adapter/inbound/web/GlobalExceptionHandler.kt` (MODIFY) | Add ConflictException → 409 |

### Subtask 5 — Tests

| File | What |
|------|------|
| `domain/QueueEntryTest.kt` | QueueEntry construction |
| `application/QueueServiceTest.kt` | enterQueue, leaveQueue, acceptCustomer with MockK |
| `application/QueueNotificationTest.kt` | Sinks pub/sub with StepVerifier |

## 8. Accept Flow Detail

```
1. Verify agent exists + agent.isAvailable() → 409 if not
2. queueRepository.removeAtomically(tenantId, entryId) → 404 if null (Lua script)
3. Create Channel(status=IN_PROGRESS, agentId) → channelRepository.save
4. Create Endpoint(CUSTOMER, customerName, customerContact) → endpointRepository.save
5. Create Endpoint(AGENT) → endpointRepository.save
6. agentRepository.save(agent.updateStatus(BUSY))
7. Emit QueueUpdate(ACCEPTED) + PositionUpdate(position=0) for accepted customer
8. Return AcceptResult(channelId, customerName, customerContact)
```

## 9. SecurityConfig / JWT Filter Changes

**SecurityConfig:** Add to permitAll:
```kotlin
"/api/queue/enter", "/api/queue/position/**"
// + HttpMethod.DELETE for "/api/queue/*"
```

**JwtAuthenticationWebFilter:** Refactor to pass-through (not reject 401) when no Bearer token present. Let Spring Security's `authorizeExchange` handle authorization. Add `/api/queue/enter`, `/api/queue/position/` to PUBLIC_PATHS.

## 10. Build Sequence

**Phase 1 — Domain + Ports + DTOs** (Subtask 1): All domain models, ports, DTOs. `./gradlew compileKotlin`
**Phase 2 — Adapters** (Subtask 2): Redis repo, notification adapter. `./gradlew compileKotlin`
**Phase 3 — Service** (Subtask 3): QueueService. `./gradlew compileKotlin`
**Phase 4 — Web + Security** (Subtask 4): Controller, SecurityConfig, JWT filter, exception handler. `./gradlew compileKotlin`
**Phase 5 — Tests** (Subtask 5): All tests. `./gradlew test`

## 11. Key Decisions

1. **QueueEntry Redis-only** — ephemeral queue state, no PostgreSQL table needed
2. **Dual Redis keys** (sorted set + hash) — O(1) entryId→member lookup for ZRANK
3. **Lua script for atomic accept** — prevents double-accept race condition
4. **JWT filter pass-through** — no Bearer → pass through, let SecurityConfig authorize
5. **In-memory Sinks** — single-instance SSE; QueueNotificationPort abstraction allows Redis Pub/Sub swap later
6. **position=0 terminal signal** — customer stream completes on accept/leave
7. **No WebSocket** — SSE simpler, sufficient for unidirectional queue updates
