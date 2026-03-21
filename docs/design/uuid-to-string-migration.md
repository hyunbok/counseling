# UUID to String Migration Plan

Date: 2026-03-21
Status: Draft

## 1. Motivation

Spring Data R2DBC uses `id == null` to distinguish INSERT (new entity) from UPDATE (existing entity). With `UUID` as the PK type, the id is always non-null (set by the caller before save), so Spring Data always attempts an UPDATE. This forces every repository to implement a manual `upsert()` via raw SQL with `ON CONFLICT DO UPDATE`.

Changing PK fields to `String? = null` lets Spring Data auto-detect insert vs update. `save()` works correctly out of the box, and DB-generated defaults (`gen_random_uuid()::text`) handle id assignment on INSERT. This eliminates all upsert-only custom repositories.

---

## 2. Database Migrations

### 2.1 Meta DB Migration: V002__uuid_to_varchar.sql

Location: `be/api/src/main/resources/db/migration/V002__uuid_to_varchar.sql`

```sql
-- Meta DB: Convert UUID columns to VARCHAR(36)

-- tenants
ALTER TABLE tenants ALTER COLUMN id TYPE VARCHAR(36) USING id::text;
ALTER TABLE tenants ALTER COLUMN id SET DEFAULT gen_random_uuid()::text;

-- super_admins
ALTER TABLE super_admins ALTER COLUMN id TYPE VARCHAR(36) USING id::text;
ALTER TABLE super_admins ALTER COLUMN id SET DEFAULT gen_random_uuid()::text;
```

### 2.2 Tenant DB Migration: V015__uuid_to_varchar.sql

Location: `be/api/src/main/resources/db/tenant-migration/V015__uuid_to_varchar.sql`

```sql
-- Tenant DB: Convert all UUID columns to VARCHAR(36)
-- Order: drop FKs -> alter columns -> re-add FKs

-- ============================================================
-- Step 1: Drop all foreign key constraints
-- ============================================================
ALTER TABLE agents DROP CONSTRAINT IF EXISTS agents_group_id_fkey;
ALTER TABLE channels DROP CONSTRAINT IF EXISTS channels_agent_id_fkey;
ALTER TABLE endpoints DROP CONSTRAINT IF EXISTS endpoints_channel_id_fkey;
ALTER TABLE chat_messages DROP CONSTRAINT IF EXISTS chat_messages_channel_id_fkey;
ALTER TABLE counsel_notes DROP CONSTRAINT IF EXISTS counsel_notes_channel_id_fkey;
ALTER TABLE counsel_notes DROP CONSTRAINT IF EXISTS counsel_notes_agent_id_fkey;
ALTER TABLE feedbacks DROP CONSTRAINT IF EXISTS feedbacks_channel_id_fkey;
ALTER TABLE recordings DROP CONSTRAINT IF EXISTS recordings_channel_id_fkey;
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_recipient_id_fkey;  -- no FK exists, skip
ALTER TABLE shared_files DROP CONSTRAINT IF EXISTS shared_files_channel_id_fkey;
ALTER TABLE screen_captures DROP CONSTRAINT IF EXISTS screen_captures_channel_id_fkey;
ALTER TABLE screen_captures DROP CONSTRAINT IF EXISTS screen_captures_captured_by_fkey;
ALTER TABLE co_browsing_sessions DROP CONSTRAINT IF EXISTS co_browsing_sessions_channel_id_fkey;
ALTER TABLE co_browsing_sessions DROP CONSTRAINT IF EXISTS co_browsing_sessions_initiated_by_fkey;

-- ============================================================
-- Step 2: Convert PK columns (UUID -> VARCHAR(36))
-- ============================================================
ALTER TABLE agents ALTER COLUMN id TYPE VARCHAR(36) USING id::text;
ALTER TABLE agents ALTER COLUMN id SET DEFAULT gen_random_uuid()::text;

ALTER TABLE groups ALTER COLUMN id TYPE VARCHAR(36) USING id::text;
ALTER TABLE groups ALTER COLUMN id SET DEFAULT gen_random_uuid()::text;

ALTER TABLE companies ALTER COLUMN id TYPE VARCHAR(36) USING id::text;
ALTER TABLE companies ALTER COLUMN id SET DEFAULT gen_random_uuid()::text;

ALTER TABLE channels ALTER COLUMN id TYPE VARCHAR(36) USING id::text;
ALTER TABLE channels ALTER COLUMN id SET DEFAULT gen_random_uuid()::text;

ALTER TABLE endpoints ALTER COLUMN id TYPE VARCHAR(36) USING id::text;
ALTER TABLE endpoints ALTER COLUMN id SET DEFAULT gen_random_uuid()::text;

ALTER TABLE chat_messages ALTER COLUMN id TYPE VARCHAR(36) USING id::text;
ALTER TABLE chat_messages ALTER COLUMN id SET DEFAULT gen_random_uuid()::text;

ALTER TABLE counsel_notes ALTER COLUMN id TYPE VARCHAR(36) USING id::text;
ALTER TABLE counsel_notes ALTER COLUMN id SET DEFAULT gen_random_uuid()::text;

ALTER TABLE feedbacks ALTER COLUMN id TYPE VARCHAR(36) USING id::text;
ALTER TABLE feedbacks ALTER COLUMN id SET DEFAULT gen_random_uuid()::text;

ALTER TABLE recordings ALTER COLUMN id TYPE VARCHAR(36) USING id::text;
ALTER TABLE recordings ALTER COLUMN id SET DEFAULT gen_random_uuid()::text;

ALTER TABLE notifications ALTER COLUMN id TYPE VARCHAR(36) USING id::text;
ALTER TABLE notifications ALTER COLUMN id SET DEFAULT gen_random_uuid()::text;

ALTER TABLE shared_files ALTER COLUMN id TYPE VARCHAR(36) USING id::text;
ALTER TABLE shared_files ALTER COLUMN id SET DEFAULT gen_random_uuid()::text;

ALTER TABLE screen_captures ALTER COLUMN id TYPE VARCHAR(36) USING id::text;
ALTER TABLE screen_captures ALTER COLUMN id SET DEFAULT gen_random_uuid()::text;

ALTER TABLE co_browsing_sessions ALTER COLUMN id TYPE VARCHAR(36) USING id::text;
ALTER TABLE co_browsing_sessions ALTER COLUMN id SET DEFAULT gen_random_uuid()::text;

-- ============================================================
-- Step 3: Convert FK columns (UUID -> VARCHAR(36))
-- ============================================================
ALTER TABLE agents ALTER COLUMN group_id TYPE VARCHAR(36) USING group_id::text;

ALTER TABLE channels ALTER COLUMN agent_id TYPE VARCHAR(36) USING agent_id::text;

ALTER TABLE endpoints ALTER COLUMN channel_id TYPE VARCHAR(36) USING channel_id::text;

ALTER TABLE chat_messages ALTER COLUMN channel_id TYPE VARCHAR(36) USING channel_id::text;

ALTER TABLE counsel_notes ALTER COLUMN channel_id TYPE VARCHAR(36) USING channel_id::text;
ALTER TABLE counsel_notes ALTER COLUMN agent_id TYPE VARCHAR(36) USING agent_id::text;

ALTER TABLE feedbacks ALTER COLUMN channel_id TYPE VARCHAR(36) USING channel_id::text;

ALTER TABLE recordings ALTER COLUMN channel_id TYPE VARCHAR(36) USING channel_id::text;

ALTER TABLE notifications ALTER COLUMN recipient_id TYPE VARCHAR(36) USING recipient_id::text;
ALTER TABLE notifications ALTER COLUMN reference_id TYPE VARCHAR(36) USING reference_id::text;

ALTER TABLE shared_files ALTER COLUMN channel_id TYPE VARCHAR(36) USING channel_id::text;

ALTER TABLE screen_captures ALTER COLUMN channel_id TYPE VARCHAR(36) USING channel_id::text;
ALTER TABLE screen_captures ALTER COLUMN captured_by TYPE VARCHAR(36) USING captured_by::text;

ALTER TABLE co_browsing_sessions ALTER COLUMN channel_id TYPE VARCHAR(36) USING channel_id::text;
ALTER TABLE co_browsing_sessions ALTER COLUMN initiated_by TYPE VARCHAR(36) USING initiated_by::text;

-- ============================================================
-- Step 4: Re-add foreign key constraints
-- ============================================================
ALTER TABLE agents ADD CONSTRAINT agents_group_id_fkey FOREIGN KEY (group_id) REFERENCES groups(id);
ALTER TABLE channels ADD CONSTRAINT channels_agent_id_fkey FOREIGN KEY (agent_id) REFERENCES agents(id);
ALTER TABLE endpoints ADD CONSTRAINT endpoints_channel_id_fkey FOREIGN KEY (channel_id) REFERENCES channels(id);
ALTER TABLE chat_messages ADD CONSTRAINT chat_messages_channel_id_fkey FOREIGN KEY (channel_id) REFERENCES channels(id);
ALTER TABLE counsel_notes ADD CONSTRAINT counsel_notes_channel_id_fkey FOREIGN KEY (channel_id) REFERENCES channels(id);
ALTER TABLE counsel_notes ADD CONSTRAINT counsel_notes_agent_id_fkey FOREIGN KEY (agent_id) REFERENCES agents(id);
ALTER TABLE feedbacks ADD CONSTRAINT feedbacks_channel_id_fkey FOREIGN KEY (channel_id) REFERENCES channels(id);
ALTER TABLE recordings ADD CONSTRAINT recordings_channel_id_fkey FOREIGN KEY (channel_id) REFERENCES channels(id);
ALTER TABLE shared_files ADD CONSTRAINT shared_files_channel_id_fkey FOREIGN KEY (channel_id) REFERENCES channels(id);
ALTER TABLE screen_captures ADD CONSTRAINT screen_captures_channel_id_fkey FOREIGN KEY (channel_id) REFERENCES channels(id);
ALTER TABLE screen_captures ADD CONSTRAINT screen_captures_captured_by_fkey FOREIGN KEY (captured_by) REFERENCES agents(id);
ALTER TABLE co_browsing_sessions ADD CONSTRAINT co_browsing_sessions_channel_id_fkey FOREIGN KEY (channel_id) REFERENCES channels(id);
ALTER TABLE co_browsing_sessions ADD CONSTRAINT co_browsing_sessions_initiated_by_fkey FOREIGN KEY (initiated_by) REFERENCES agents(id);

-- ============================================================
-- Step 5: Indexes are automatically preserved (they work on
-- the column regardless of type). No index rebuild needed.
-- The partial/unique indexes and their WHERE clauses remain valid.
-- ============================================================
```

Notes on feedbacks.channel_id UNIQUE constraint: The UNIQUE constraint on `feedbacks.channel_id` is preserved automatically during type conversion. No need to drop/recreate it.

---

## 3. Code Changes (Ordered)

### Phase 1: Domain Models (both modules)

Change `id: UUID` to `id: String? = null` for all entities. Change all UUID FK fields to `String` (non-nullable) or `String?` (nullable). Remove `import java.util.UUID` where no longer needed.

#### be/api module - Domain Models (16 entities)

| File | id change | FK field changes |
|------|-----------|-----------------|
| `domain/Agent.kt` | `id: UUID` -> `id: String? = null` | `groupId: UUID?` -> `String?`; `assignToGroup(groupId: UUID?)` -> `String?` |
| `domain/Channel.kt` | `id: UUID` -> `id: String? = null` | `agentId: UUID?` -> `String?`; `assignAgent(agentId: UUID)` -> `String` |
| `domain/ChatMessage.kt` | `id: UUID` -> `id: String? = null` | `channelId: UUID` -> `String` |
| `domain/CoBrowsingSession.kt` | `id: UUID` -> `id: String? = null` | `channelId: UUID` -> `String`; `initiatedBy: UUID` -> `String` |
| `domain/Company.kt` | `id: UUID` -> `id: String? = null` | (none) |
| `domain/CounselNote.kt` | `id: UUID` -> `id: String? = null` | `channelId: UUID` -> `String`; `agentId: UUID` -> `String` |
| `domain/Endpoint.kt` | `id: UUID` -> `id: String? = null` | `channelId: UUID` -> `String` |
| `domain/Feedback.kt` | `id: UUID` -> `id: String? = null` | `channelId: UUID` -> `String` |
| `domain/Group.kt` | `id: UUID` -> `id: String? = null` | (none) |
| `domain/Notification.kt` | `id: UUID` -> `id: String? = null` | `recipientId: UUID` -> `String`; `referenceId: UUID?` -> `String?` |
| `domain/Recording.kt` | `id: UUID` -> `id: String? = null` | `channelId: UUID` -> `String` |
| `domain/ScreenCapture.kt` | `id: UUID` -> `id: String? = null` | `channelId: UUID` -> `String`; `capturedBy: UUID` -> `String` |
| `domain/SharedFile.kt` | `id: UUID` -> `id: String? = null` | `channelId: UUID` -> `String` |
| `domain/Tenant.kt` | `id: UUID` -> `id: String? = null` | (none) |
| `domain/SuperAdmin.kt` | `id: UUID` -> `id: String? = null` | (none) |
| `domain/QueueEntry.kt` | `id: UUID` -> `id: String? = null` | `groupId: UUID?` -> `String?` |

Auth domain (not persisted directly, but carries agent/admin id):

| File | Change |
|------|--------|
| `domain/auth/AuthenticatedAgent.kt` | `agentId: UUID` -> `String` |
| `domain/auth/JwtClaims.kt` | `subject: UUID` -> `String` |

#### be/api-admin module - Domain Models (7 entities)

| File | id change | FK field changes |
|------|-----------|-----------------|
| `domain/Agent.kt` | `id: UUID` -> `id: String? = null` | `groupId: UUID?` -> `String?`; `assignToGroup(groupId: UUID?)` -> `String?` |
| `domain/Channel.kt` | `id: UUID` -> `id: String? = null` | `agentId: UUID?` -> `String?` |
| `domain/Company.kt` | `id: UUID` -> `id: String? = null` | (none) |
| `domain/Feedback.kt` | `id: UUID` -> `id: String? = null` | `channelId: UUID` -> `String` |
| `domain/Group.kt` | `id: UUID` -> `id: String? = null` | (none) |
| `domain/SuperAdmin.kt` | `id: UUID` -> `id: String? = null` | (none) |
| `domain/Tenant.kt` | `id: UUID` -> `id: String? = null` | (none) |

Admin auth domain:

| File | Change |
|------|--------|
| `domain/auth/AuthenticatedAdmin.kt` | `adminId: UUID` -> `String` |
| `domain/auth/AdminJwtClaims.kt` | `subject: UUID` -> `String` |

#### Domain Events (no changes needed)

Domain events carry domain model instances (e.g., `Agent`, `Channel`), so they inherit the type change automatically. No direct UUID references.

### Phase 2: Port Interfaces

#### be/api - Repository Interfaces (change generic type parameter)

All `ReactiveCrudRepository<Entity, UUID>` -> `ReactiveCrudRepository<Entity, String>`.
All method parameters `id: UUID` -> `id: String`, `channelId: UUID` -> `channelId: String`, etc.

| File | Changes |
|------|---------|
| `port/outbound/tenant/AgentRepository.kt` | `<Agent, UUID>` -> `<Agent, String>`; all UUID params -> String |
| `port/outbound/tenant/ChannelRepository.kt` | `<Channel, UUID>` -> `<Channel, String>`; all UUID params -> String |
| `port/outbound/tenant/ChatMessageRepository.kt` | `<ChatMessage, UUID>` -> `<ChatMessage, String>`; `channelId: UUID` -> String |
| `port/outbound/tenant/CoBrowsingSessionRepository.kt` | `<CoBrowsingSession, UUID>` -> `<CoBrowsingSession, String>`; all UUID params -> String |
| `port/outbound/tenant/CompanyRepository.kt` | `<Company, UUID>` -> `<Company, String>` |
| `port/outbound/tenant/CounselNoteRepository.kt` | `<CounselNote, UUID>` -> `<CounselNote, String>`; all UUID params -> String |
| `port/outbound/tenant/EndpointRepository.kt` | `<Endpoint, UUID>` -> `<Endpoint, String>`; `channelId: UUID` -> String |
| `port/outbound/tenant/FeedbackRepository.kt` | `<Feedback, UUID>` -> `<Feedback, String>`; `channelId: UUID` -> String |
| `port/outbound/tenant/GroupRepository.kt` | `<Group, UUID>` -> `<Group, String>`; `id: UUID` -> String |
| `port/outbound/tenant/NotificationRepository.kt` | `<Notification, UUID>` -> `<Notification, String>`; all UUID params -> String |
| `port/outbound/tenant/RecordingRepository.kt` | `<Recording, UUID>` -> `<Recording, String>`; all UUID params -> String |
| `port/outbound/tenant/ScreenCaptureRepository.kt` | `<ScreenCapture, UUID>` -> `<ScreenCapture, String>`; `id: UUID` -> String |
| `port/outbound/tenant/SharedFileRepository.kt` | `<SharedFile, UUID>` -> `<SharedFile, String>`; `id: UUID` -> String |
| `port/outbound/meta/TenantRepository.kt` | `<Tenant, UUID>` -> `<Tenant, String>`; all UUID params -> String |

Remove `*RepositoryCustom` from inheritance for repositories that will lose their custom interface (see Phase 3).

#### be/api-admin - Repository Interfaces

| File | Changes |
|------|---------|
| `port/outbound/AdminAgentRepository.kt` | `<Agent, UUID>` -> `<Agent, String>`; all UUID params -> String |
| `port/outbound/AdminChannelRepository.kt` | `<Channel, UUID>` -> `<Channel, String>` |
| `port/outbound/AdminCompanyRepository.kt` | `<Company, UUID>` -> `<Company, String>` |
| `port/outbound/AdminGroupRepository.kt` | `<Group, UUID>` -> `<Group, String>`; all UUID params -> String |
| `port/outbound/meta/AdminSuperAdminRepository.kt` | `<SuperAdmin, UUID>` -> `<SuperAdmin, String>` |
| `port/outbound/meta/AdminTenantRepository.kt` | `<Tenant, UUID>` -> `<Tenant, String>`; all UUID params -> String |
| `port/outbound/AdminFeedbackRepository.kt` | `id: UUID` -> String; `agentId: UUID?` -> String? |
| `port/outbound/AdminStatsRepository.kt` | (no UUID in interface, but AgentStats has UUID -- see below) |

#### be/api - Inbound Ports

All use case and query interfaces with UUID parameters/fields change to String:

| File | UUID params/fields to change |
|------|------------------------------|
| `port/inbound/ChannelUseCase.kt` | All UUID params |
| `port/inbound/ChatUseCase.kt` | All UUID params |
| `port/inbound/ChatQuery.kt` | All UUID params |
| `port/inbound/CoBrowsingUseCase.kt` | `channelId`, `agentId`, `sessionId` in command data classes |
| `port/inbound/CoBrowsingQuery.kt` | `channelId: UUID` -> String |
| `port/inbound/FeedbackUseCase.kt` | All UUID params |
| `port/inbound/FeedbackQuery.kt` | `channelId: UUID` -> String |
| `port/inbound/HistoryQuery.kt` | All UUID fields in filter/result data classes |
| `port/inbound/NotificationUseCase.kt` | All UUID params |
| `port/inbound/NotificationQuery.kt` | All UUID params |
| `port/inbound/QueueUseCase.kt` | All UUID params |
| `port/inbound/RecordingUseCase.kt` | All UUID params |
| `port/inbound/RecordingStreamUseCase.kt` | All UUID params |
| `port/inbound/ScreenCaptureUseCase.kt` | `channelId`, `capturedBy`, `captureId` |
| `port/inbound/ScreenCaptureQuery.kt` | `channelId: UUID` -> String |
| `port/inbound/SharedFileUseCase.kt` | `channelId`, `fileId` |
| `port/inbound/SharedFileQuery.kt` | All UUID params |
| `port/inbound/AuthUseCase.kt` | All UUID params |

#### be/api-admin - Inbound Ports

| File | UUID params/fields to change |
|------|------------------------------|
| `port/inbound/AgentManagementUseCase.kt` | All UUID params |
| `port/inbound/GroupManagementUseCase.kt` | All UUID params |
| `port/inbound/CompanyManagementUseCase.kt` | All UUID params |
| `port/inbound/TenantManagementUseCase.kt` | All UUID params |
| `port/inbound/FeedbackQuery.kt` | `agentId: UUID?` -> String?; `id: UUID` -> String |
| `port/inbound/StatsQuery.kt` | `AgentStats.agentId: UUID` -> String |
| `port/inbound/MonitoringQuery.kt` | (already uses String for agentId -- no change) |
| `port/inbound/AdminAuthUseCase.kt` | All UUID params |

### Phase 3: Delete Upsert-Only Custom Repositories

These custom repositories exist ONLY for `upsert()`. After the migration, `save()` handles both insert and update. Delete both the interface and implementation.

#### be/api module -- FILES TO DELETE (24 files)

Custom interfaces to delete:
1. `be/api/src/main/kotlin/com/counseling/api/port/outbound/tenant/AgentRepositoryCustom.kt`
2. `be/api/src/main/kotlin/com/counseling/api/port/outbound/tenant/ChannelRepositoryCustom.kt`
3. `be/api/src/main/kotlin/com/counseling/api/port/outbound/tenant/ChatMessageRepositoryCustom.kt`
4. `be/api/src/main/kotlin/com/counseling/api/port/outbound/tenant/CoBrowsingSessionRepositoryCustom.kt`
5. `be/api/src/main/kotlin/com/counseling/api/port/outbound/tenant/CompanyRepositoryCustom.kt`
6. `be/api/src/main/kotlin/com/counseling/api/port/outbound/tenant/CounselNoteRepositoryCustom.kt`
7. `be/api/src/main/kotlin/com/counseling/api/port/outbound/tenant/EndpointRepositoryCustom.kt`
8. `be/api/src/main/kotlin/com/counseling/api/port/outbound/tenant/GroupRepositoryCustom.kt`
9. `be/api/src/main/kotlin/com/counseling/api/port/outbound/tenant/RecordingRepositoryCustom.kt`
10. `be/api/src/main/kotlin/com/counseling/api/port/outbound/tenant/ScreenCaptureRepositoryCustom.kt`
11. `be/api/src/main/kotlin/com/counseling/api/port/outbound/tenant/SharedFileRepositoryCustom.kt`
12. `be/api/src/main/kotlin/com/counseling/api/port/outbound/meta/TenantRepositoryCustom.kt`

Custom implementations to delete:
13. `be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/AgentRepositoryCustomImpl.kt`
14. `be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/ChannelRepositoryCustomImpl.kt`
15. `be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/ChatMessageRepositoryCustomImpl.kt`
16. `be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/CoBrowsingSessionRepositoryCustomImpl.kt`
17. `be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/CompanyRepositoryCustomImpl.kt`
18. `be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/CounselNoteRepositoryCustomImpl.kt`
19. `be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/EndpointRepositoryCustomImpl.kt`
20. `be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/GroupRepositoryCustomImpl.kt`
21. `be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/RecordingRepositoryCustomImpl.kt`
22. `be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/ScreenCaptureRepositoryCustomImpl.kt`
23. `be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/SharedFileRepositoryCustomImpl.kt`
24. `be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/TenantRepositoryCustomImpl.kt`

#### be/api module -- Custom Repos to KEEP (modified, not deleted)

These have special SQL behavior beyond simple upsert:

| File | Reason to keep | Changes needed |
|------|---------------|----------------|
| `FeedbackRepositoryCustom.kt` + `FeedbackRepositoryCustomImpl.kt` | `ON CONFLICT (channel_id) DO NOTHING` -- prevents duplicate feedback per channel. `save()` cannot replicate this. | Rename method from `upsert()` to `insertIgnoreDuplicate()`. Change UUID bindings to String. |
| `NotificationRepositoryCustom.kt` + `NotificationRepositoryCustomImpl.kt` | `RETURNING *` -- returns the DB-generated row with server defaults. Needed for getting the DB-assigned id. | Rename method from `upsert()` to `insertReturning()`. Change `UUID.class` to `String.class` in row mapper. |

#### be/api-admin module -- FILES TO DELETE (2 files)

The admin module's `AdminCompanyRepositoryCustom` only has `upsert()`:
1. `be/api-admin/src/main/kotlin/com/counseling/admin/port/outbound/AdminCompanyRepositoryCustom.kt`
2. `be/api-admin/src/main/kotlin/com/counseling/admin/adapter/outbound/persistence/AdminCompanyRepositoryCustomImpl.kt`

#### be/api-admin module -- Custom Repos to KEEP (modified)

These have dynamic search/pagination beyond upsert:

| File pair | Reason | Changes |
|-----------|--------|---------|
| `AdminAgentRepositoryCustom.kt` + `AdminAgentRepositoryCustomImpl.kt` | Dynamic search, pagination, `upsert()` | Remove `upsert()` method. Change `UUID.class` to `String.class` in mappers. Change `groupId: UUID` -> `String` params. |
| `AdminGroupRepositoryCustom.kt` + `AdminGroupRepositoryCustomImpl.kt` | Dynamic search, pagination, `upsert()` | Remove `upsert()` method. Change `UUID.class` to `String.class` in mapper. |
| `AdminTenantRepositoryCustom.kt` + `AdminTenantRepositoryCustomImpl.kt` | Dynamic search, pagination, `upsert()` | Remove `upsert()` method. Change `UUID.class` to `String.class` in mapper. |

### Phase 4: Service Layer Changes

Replace all `.upsert(entity)` calls with `.save(entity)` across all services.

#### be/api module (24 call sites in ~10 service files)

| Service file | # of upsert calls | Action |
|-------------|-------------------|--------|
| `application/AuthService.kt` | 2 | `.upsert(...)` -> `.save(...)` |
| `application/ChannelService.kt` | 3 | `.upsert(...)` -> `.save(...)` |
| `application/ChatService.kt` | 1 | `.upsert(...)` -> `.save(...)` |
| `application/CoBrowsingService.kt` | 3 | `.upsert(...)` -> `.save(...)` |
| `application/FeedbackService.kt` | 1 | `.upsert(...)` -> `.insertIgnoreDuplicate(...)` (special case) |
| `application/NotificationService.kt` | 1 | `.upsert(...)` -> `.insertReturning(...)` (special case) |
| `application/QueueService.kt` | 5 | `.upsert(...)` -> `.save(...)` |
| `application/RecordingService.kt` | 3 | `.upsert(...)` -> `.save(...)` |
| `application/ScreenCaptureService.kt` | 1 | `.upsert(...)` -> `.save(...)` |
| `application/SharedFileService.kt` | 1 | `.upsert(...)` -> `.save(...)` |
| `adapter/inbound/web/controller/AgentController.kt` | 1 | `.upsert(...)` -> `.save(...)` |

Also: Change all `UUID.randomUUID()` to `null` (let DB generate) or remove id construction entirely, since `id: String? = null` means the entity starts with null id.

For entities created in code, the pattern changes from:
```kotlin
// BEFORE
val channel = Channel(id = UUID.randomUUID(), ...)
channelRepository.upsert(channel)

// AFTER
val channel = Channel(agentId = ..., status = ..., ...)  // id defaults to null
channelRepository.save(channel)  // Spring Data does INSERT, DB generates id
```

For entities that are loaded-then-updated, `save()` works because the loaded entity already has a non-null id, so Spring Data does UPDATE.

#### be/api-admin module (11 call sites in 4 service files)

| Service file | # of upsert calls | Action |
|-------------|-------------------|--------|
| `application/AgentManagementService.kt` | 4 | `.upsert(...)` -> `.save(...)` |
| `application/CompanyManagementService.kt` | 1 | `.upsert(...)` -> `.save(...)` |
| `application/GroupManagementService.kt` | 3 | `.upsert(...)` -> `.save(...)` |
| `application/TenantManagementService.kt` | 3 | `.upsert(...)` -> `.save(...)` |

### Phase 5: Controller / DTO Changes

#### be/api module

All `@PathVariable id: UUID` -> `@PathVariable id: String` in controllers.
All UUID fields in request/response DTOs -> String.

| Controller | UUID path params | UUID in DTOs |
|-----------|-----------------|--------------|
| `ChannelController.kt` | channelId, agentId | ChannelDtos |
| `ChatController.kt` | channelId | ChatDtos |
| `CoBrowsingController.kt` | channelId, sessionId | CoBrowsingDtos |
| `FeedbackController.kt` | channelId | FeedbackDtos |
| `HistoryController.kt` | (query params) | HistoryDtos |
| `NotificationController.kt` | id, recipientId | NotificationDtos |
| `QueueController.kt` | (various) | QueueDtos |
| `RecordingController.kt` | channelId, recordingId | RecordingDtos |
| `ScreenCaptureController.kt` | channelId, captureId | ScreenCaptureDtos |
| `SharedFileController.kt` | channelId, fileId | SharedFileDtos |
| `AgentController.kt` | (via auth) | AuthDtos |
| `AuthController.kt` | (via auth) | AuthDtos |

#### be/api-admin module

Same pattern: all UUID path params and DTO fields -> String.

| Controller | UUID path params |
|-----------|-----------------|
| `AgentController.kt` | agentId |
| `CompanyController.kt` | (none) |
| `FeedbackController.kt` | feedbackId, agentId query param |
| `GroupController.kt` | groupId |
| `MonitoringController.kt` | (none) |
| `StatsController.kt` | (none) |
| `TenantController.kt` | tenantId |
| `AdminAuthController.kt` | (none) |

### Phase 6: External Adapter Changes

#### be/api module

| File | Changes |
|------|---------|
| `adapter/outbound/persistence/FeedbackRepositoryCustomImpl.kt` | Rename `upsert` -> `insertIgnoreDuplicate`. Change `.bind("id", feedback.id)` to `.bind("id", feedback.id ?: ...)`. Change `UUID::class.java` -> `String::class.java` in bindNull. |
| `adapter/outbound/persistence/NotificationRepositoryCustomImpl.kt` | Rename `upsert` -> `insertReturning`. Change `row.get("id", UUID::class.java)` -> `row.get("id", String::class.java)`. Change all UUID mapper calls. |
| `adapter/outbound/persistence/HistoryMongoRepository.kt` | Check for UUID references in MongoDB queries. |
| `adapter/outbound/external/JjwtTokenProviderTest.kt` | UUID -> String in JWT claims construction. |
| JWT-related adapters | `subject: UUID` -> `subject: String` in token creation/parsing. |

#### be/api-admin module

| File | Changes |
|------|---------|
| `AdminAgentRepositoryCustomImpl.kt` | Remove `upsert()`. Change `UUID::class.java` -> `String::class.java` in mappers. |
| `AdminGroupRepositoryCustomImpl.kt` | Remove `upsert()`. Change `UUID::class.java` -> `String::class.java` in mapper. |
| `AdminTenantRepositoryCustomImpl.kt` | Remove `upsert()`. Change `UUID::class.java` -> `String::class.java` in mapper. |
| `AdminFeedbackR2dbcRepository.kt` | Change `UUID::class.java` -> `String::class.java` in mappers and bindings. |
| `AdminStatsR2dbcRepository.kt` | Change `row.get("agent_id", UUID::class.java)` -> `String::class.java`. |
| `AdminTenantConnectionRegistryImpl.kt` | Check for UUID references. |

### Phase 7: Test Changes

All test files using `UUID.randomUUID()` -> `UUID.randomUUID().toString()` for creating test data, or use `null` for new entities.

Affected test files (37 files in be/api, 0 in be/api-admin -- admin has no tests currently):
- All mock method signatures change from `UUID` -> `String`
- All test entity construction: `id = UUID.randomUUID()` -> `id = UUID.randomUUID().toString()` (for loaded entities) or `id = null` (for new entities)

---

## 4. Detailed File Inventory

### Total files affected

| Category | be/api | be/api-admin | Total |
|----------|--------|-------------|-------|
| Domain models | 17 | 9 | 26 |
| Port interfaces (outbound) | 14 | 7 | 21 |
| Port interfaces (inbound) | 18 | 6 | 24 |
| Custom impls (delete) | 12 | 1 | 13 |
| Custom interfaces (delete) | 12 | 1 | 13 |
| Custom impls (modify) | 2 | 4 | 6 |
| Custom interfaces (modify) | 2 | 3 | 5 |
| Services/application | 11 | 4 | 15 |
| Controllers | 12 | 8 | 20 |
| DTOs | 11 | 6 | 17 |
| Tests | 37 | 0 | 37 |
| SQL migrations | 1 | 0 | 1 (shared) |
| SQL migrations (meta) | 1 | 0 | 1 |
| **Total** | **~150** | **~49** | **~199** |

### Files to DELETE (26 total)

```
# be/api -- Custom interfaces (upsert-only)
be/api/src/main/kotlin/com/counseling/api/port/outbound/tenant/AgentRepositoryCustom.kt
be/api/src/main/kotlin/com/counseling/api/port/outbound/tenant/ChannelRepositoryCustom.kt
be/api/src/main/kotlin/com/counseling/api/port/outbound/tenant/ChatMessageRepositoryCustom.kt
be/api/src/main/kotlin/com/counseling/api/port/outbound/tenant/CoBrowsingSessionRepositoryCustom.kt
be/api/src/main/kotlin/com/counseling/api/port/outbound/tenant/CompanyRepositoryCustom.kt
be/api/src/main/kotlin/com/counseling/api/port/outbound/tenant/CounselNoteRepositoryCustom.kt
be/api/src/main/kotlin/com/counseling/api/port/outbound/tenant/EndpointRepositoryCustom.kt
be/api/src/main/kotlin/com/counseling/api/port/outbound/tenant/GroupRepositoryCustom.kt
be/api/src/main/kotlin/com/counseling/api/port/outbound/tenant/RecordingRepositoryCustom.kt
be/api/src/main/kotlin/com/counseling/api/port/outbound/tenant/ScreenCaptureRepositoryCustom.kt
be/api/src/main/kotlin/com/counseling/api/port/outbound/tenant/SharedFileRepositoryCustom.kt
be/api/src/main/kotlin/com/counseling/api/port/outbound/meta/TenantRepositoryCustom.kt

# be/api -- Custom implementations (upsert-only)
be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/AgentRepositoryCustomImpl.kt
be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/ChannelRepositoryCustomImpl.kt
be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/ChatMessageRepositoryCustomImpl.kt
be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/CoBrowsingSessionRepositoryCustomImpl.kt
be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/CompanyRepositoryCustomImpl.kt
be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/CounselNoteRepositoryCustomImpl.kt
be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/EndpointRepositoryCustomImpl.kt
be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/GroupRepositoryCustomImpl.kt
be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/RecordingRepositoryCustomImpl.kt
be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/ScreenCaptureRepositoryCustomImpl.kt
be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/SharedFileRepositoryCustomImpl.kt
be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/TenantRepositoryCustomImpl.kt

# be/api-admin -- Custom (upsert-only)
be/api-admin/src/main/kotlin/com/counseling/admin/port/outbound/AdminCompanyRepositoryCustom.kt
be/api-admin/src/main/kotlin/com/counseling/admin/adapter/outbound/persistence/AdminCompanyRepositoryCustomImpl.kt
```

---

## 5. Risk Assessment

### High Risk

| Risk | Impact | Mitigation |
|------|--------|------------|
| Data loss during column type conversion | Existing UUID values could be truncated or lost | `USING id::text` explicitly casts; UUID string representation is exactly 36 chars; VARCHAR(36) is sufficient. Test migration on staging DB first. |
| FK constraint violations during migration | If FKs are dropped and re-added in wrong order, orphan references may fail | The migration drops ALL FKs first, converts ALL columns, then re-adds FKs. Run in a single transaction. |
| Multi-tenant migration coordination | Each tenant DB needs V015 independently | Flyway tenant migration already handles this via `TenantBootstrap`. All tenant DBs get the same migration. |

### Medium Risk

| Risk | Impact | Mitigation |
|------|--------|------------|
| Spring Data `save()` doing SELECT before INSERT | Performance regression on INSERT path (extra round trip) | Spring Data R2DBC checks `id == null` -> INSERT. Since we use `id: String? = null`, new entities have null id and go straight to INSERT. No SELECT needed. |
| JWT tokens in flight become invalid | Existing JWTs have UUID-format subject; new code expects String | UUID string format (`550e8400-e29b-...`) is already a valid String. Parsing logic just changes from `UUID.fromString(subject)` to using the string directly. No format change. |
| Redis cached agent statuses reference UUID keys | Cache keys may use UUID format | Redis keys are tenant-scoped strings (`AgentStatusCacheRepository` uses `tenantSlug`). Agent status values already use String keys. Verify with actual Redis data. |
| MongoDB documents may contain UUID fields | History/query documents in MongoDB | Check `HistoryMongoRepository` and any MongoDB document models for UUID references. These need String conversion too. |

### Low Risk

| Risk | Impact | Mitigation |
|------|--------|------------|
| Index performance on VARCHAR(36) vs UUID | UUID is 16 bytes; VARCHAR(36) is ~37 bytes. Index slightly larger. | For this workload size, the difference is negligible. VARCHAR(36) with btree indexes performs well. |
| Existing data consistency | Old UUID strings might have inconsistent casing | PostgreSQL `gen_random_uuid()::text` always produces lowercase. Existing data via `USING id::text` also produces lowercase. Consistent. |

---

## 6. Implementation Order

Execute in this exact sequence:

1. **SQL migrations** -- Write and test V002 (meta) and V015 (tenant) on a staging DB
2. **Domain models** -- All 26 entity files (both modules)
3. **Auth domain** -- JwtClaims, AuthenticatedAgent/Admin (4 files)
4. **Delete custom repos** -- Remove 26 files (interfaces + impls)
5. **Modify kept custom repos** -- FeedbackCustom, NotificationCustom (api); Agent/Group/Tenant Custom (admin) -- remove upsert, update mappers
6. **Repository interfaces** -- Remove deleted Custom from inheritance, update type params (21 files)
7. **Inbound ports** -- Update all UUID params to String (24 files)
8. **Service layer** -- Replace `.upsert()` with `.save()`, remove `UUID.randomUUID()` for new entity creation (15 files)
9. **Controllers + DTOs** -- Update path variables and DTO fields (37 files)
10. **External adapters** -- JWT provider, MongoDB repos, Redis adapters
11. **Tests** -- Update all 37 test files
12. **Build + verify** -- `./gradlew build` for both modules

---

## 7. Verification Checklist

- [ ] `./gradlew :api:build` passes
- [ ] `./gradlew :api-admin:build` passes
- [ ] No remaining `import java.util.UUID` in domain models (except QueueEntry if used transiently)
- [ ] No remaining `ReactiveCrudRepository<*, UUID>` anywhere
- [ ] No remaining `.upsert(` calls (except renamed `insertIgnoreDuplicate` / `insertReturning`)
- [ ] SQL migration runs clean on empty DB
- [ ] SQL migration runs clean on existing DB with data
- [ ] JWT login/refresh flow works end-to-end
- [ ] Channel create/close flow works end-to-end
- [ ] Feedback submit (ON CONFLICT DO NOTHING) still works
- [ ] Notification create (RETURNING *) still returns generated id
