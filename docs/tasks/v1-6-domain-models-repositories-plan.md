# v1-6: Domain Models and Repositories — Design Document

## 1. Overview

Create domain entities, enums, events, repository ports, R2DBC implementations, and DB migrations for the counseling platform's core business models.

**Scope:** Command-side only (PostgreSQL). MongoDB read models deferred to separate task.
**All entities are tenant-scoped** — stored in tenant DBs, repositories use primary `DatabaseClient` (tenant-routed).

## 2. Agent Reconciliation

Existing `Agent` from v1-5 is EXTENDED (not replaced). Add `groupId: UUID?` and `agentStatus: AgentStatus`.
Migration V002 alters the existing `agents` table.

## 3. Domain Models

All in `com.counseling.api.domain`. Follow existing pattern: immutable `data class`, `Instant` timestamps, `softDelete()`.

| File | Type | Key Fields / Methods |
|------|------|---------------------|
| `Agent.kt` (MODIFY) | data class | +`groupId: UUID?`, +`agentStatus: AgentStatus` +`updateStatus()`, `assignToGroup()`, `isAvailable()` |
| `AgentStatus.kt` | enum | ONLINE, OFFLINE, BUSY, AWAY, WRAP_UP |
| `Group.kt` | data class | id, name, status, timestamps, deleted. `rename()`, `activate()`, `deactivate()`, `softDelete()` |
| `GroupStatus.kt` | enum | ACTIVE, INACTIVE |
| `Company.kt` | data class | id, name, contact?, address?, timestamps. `update()`. No deleted (singleton per tenant) |
| `Channel.kt` | data class | id, agentId?, status, startedAt?, endedAt?, recordingPath?, timestamps, deleted. `assignAgent()`, `start()`, `close()`, `isOpen()` |
| `ChannelStatus.kt` | enum | WAITING, IN_PROGRESS, CLOSED |
| `Endpoint.kt` | data class | id, channelId, type, customerName?, customerContact?, joinedAt, leftAt?. `leave()` |
| `EndpointType.kt` | enum | AGENT, CUSTOMER |
| `ChatMessage.kt` | data class | id, channelId, senderType, senderId, content, createdAt. Immutable (append-only) |
| `SenderType.kt` | enum | AGENT, CUSTOMER |
| `CounselNote.kt` | data class | id, channelId, agentId, content, timestamps, deleted. `updateContent()`, `softDelete()` |
| `Feedback.kt` | data class | id, channelId, rating(1-5), comment?, createdAt. `init { require(rating in 1..5) }` |
| `exception/NotFoundException.kt` | exception | 404 |

## 4. Domain Events

In `com.counseling.api.domain.event`. Pure data classes, no Spring coupling.

| File | Events |
|------|--------|
| `DomainEvent.kt` | Sealed interface: `occurredAt: Instant` |
| `AgentEvent.kt` | Sealed class: `StatusChanged(agent)`, `AssignedToGroup(agent)` |
| `ChannelEvent.kt` | Sealed class: `Created(channel)`, `Started(channel)`, `Closed(channel)` |
| `FeedbackEvent.kt` | `Submitted(feedback)` |

## 5. DB Migrations (tenant-migration/)

### V002__create_groups_and_alter_agents.sql
```sql
CREATE TABLE groups (...); ALTER TABLE agents ADD COLUMN group_id UUID REFERENCES groups(id), ADD COLUMN agent_status VARCHAR(20) DEFAULT 'OFFLINE';
```

### V003__create_company.sql
```sql
CREATE TABLE companies (id, name, contact, address, timestamps);
```

### V004__create_channels_and_endpoints.sql
```sql
CREATE TABLE channels (id, agent_id FK, status, started_at, ended_at, recording_path, timestamps, deleted);
CREATE TABLE endpoints (id, channel_id FK, type, customer_name, customer_contact, joined_at, left_at);
```

### V005__create_chat_messages.sql
```sql
CREATE TABLE chat_messages (id, channel_id FK, sender_type, sender_id, content TEXT, created_at);
```

### V006__create_counsel_notes_and_feedback.sql
```sql
CREATE TABLE counsel_notes (id, channel_id FK, agent_id FK, content TEXT, timestamps, deleted);
CREATE TABLE feedbacks (id, channel_id FK UNIQUE, rating 1-5, comment, created_at);
```

## 6. Repository Port Interfaces (port/outbound/)

| File | Methods |
|------|---------|
| `AgentRepository.kt` (MODIFY) | +`findAllByGroupIdAndNotDeleted(groupId): Flux`, +`findAllByNotDeleted(): Flux` |
| `GroupRepository.kt` | save, findByIdAndNotDeleted, findAllByNotDeleted |
| `CompanyRepository.kt` | save, findFirst |
| `ChannelRepository.kt` | save, findByIdAndNotDeleted, findAllByAgentIdAndNotDeleted, findAllByStatusAndNotDeleted |
| `EndpointRepository.kt` | save, findAllByChannelId |
| `ChatMessageRepository.kt` | save, findAllByChannelId |
| `CounselNoteRepository.kt` | save, findByIdAndNotDeleted, findAllByChannelIdAndNotDeleted |
| `FeedbackRepository.kt` | save, findByChannelId |

## 7. R2DBC Adapter Implementations (adapter/outbound/persistence/)

All follow existing pattern: `@Repository @Profile("!test")`, inject primary `DatabaseClient`, raw SQL, `mapToXxx(Readable)`.

| File | Notes |
|------|-------|
| `AgentR2dbcRepository.kt` (MODIFY) | Update mapToAgent, save SQL for new columns, add new queries |
| `GroupR2dbcRepository.kt` | CRUD on `groups` table |
| `CompanyR2dbcRepository.kt` | save (UPSERT), findFirst |
| `ChannelR2dbcRepository.kt` | CRUD on `channels` table |
| `EndpointR2dbcRepository.kt` | save, findByChannelId on `endpoints` table |
| `ChatMessageR2dbcRepository.kt` | save, findByChannelId on `chat_messages` table |
| `CounselNoteR2dbcRepository.kt` | CRUD on `counsel_notes` table |
| `FeedbackR2dbcRepository.kt` | save, findByChannelId on `feedbacks` table |

## 8. GlobalExceptionHandler Update

Add `NotFoundException` -> 404 mapping.

## 9. Tests

| File | What |
|------|------|
| `domain/AgentTest.kt` | updateStatus, assignToGroup, isAvailable |
| `domain/ChannelTest.kt` | State transitions: start, close, isOpen |
| `domain/FeedbackTest.kt` | Rating validation 1-5 |
| `domain/GroupTest.kt` | rename, activate/deactivate, softDelete |
| `domain/event/DomainEventTest.kt` | Event creation |

## 10. Build Sequence

**Phase 1 — Enums + Domain Models + Events + Exceptions** (Subtasks 1-3)
All domain files, Agent modification, events, NotFoundException. `./gradlew compileKotlin`

**Phase 2 — DB Migrations + Port Interfaces** (Subtask 4)
V002-V006 migrations, all repository port interfaces, AgentRepository extension. `./gradlew compileKotlin`

**Phase 3 — R2DBC Implementations + Tests** (Subtask 5)
All R2DBC repository adapters, AgentR2dbcRepository modification, GlobalExceptionHandler update, unit tests. `./gradlew test`

## 11. Key Decisions
1. **Agent extension** over new User entity — backward compatible via nullable defaults
2. **`groups` table** — not reserved in PostgreSQL
3. **ChatMessage append-only** — no update/delete for audit integrity
4. **Feedback one-per-channel** — UNIQUE(channel_id) constraint
5. **Endpoint no soft-delete** — lifecycle records, only `left_at` set
6. **Company singleton** — no deleted column, one per tenant
7. **MongoDB deferred** — query-side read models are a separate task
