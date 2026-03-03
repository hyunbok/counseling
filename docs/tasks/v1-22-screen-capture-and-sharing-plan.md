# v1-22: Screen Capture and Screen Sharing — Design Plan

## 1. Overview

Backend service for screen capture during counseling sessions. Agents capture PNG screenshots and upload via REST API. CQRS: PostgreSQL (write) → MongoDB (read), SSE for real-time updates. Reuses existing `FileStoragePort` from v1-21.

**Screen sharing**: LiveKit-based, frontend-only. No backend changes needed — tokens already include `canPublish` grant. Frontend agent app has no source files yet, so frontend implementation is deferred.

**Scope**: Backend only (18 new files, 2 modified).

## 2. API Design

Base: `/api/channels/{channelId}/captures`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/captures` | Agent JWT | Upload PNG capture (multipart: `image` + optional `note`) |
| GET | `/captures` | permitAll | List captures (cursor: `before`, `limit`) |
| GET | `/captures/{id}/download` | permitAll | Download PNG binary |
| DELETE | `/captures/{id}` | Agent JWT | Soft-delete capture |
| GET | `/captures/stream` | permitAll | SSE real-time stream |

**Validation**: PNG only, max 10MB, `note` max 500 chars.

## 3. Domain Model

```kotlin
data class ScreenCapture(
    val id: UUID, val channelId: UUID, val capturedBy: UUID,
    val originalFilename: String, val storedFilename: String,
    val contentType: String, val fileSize: Long, val storagePath: String,
    val note: String?, val createdAt: Instant, val deleted: Boolean = false,
)
```

## 4. Data Schemas

### PostgreSQL — `V012__create_screen_captures.sql`
```sql
CREATE TABLE screen_captures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id UUID NOT NULL REFERENCES channels(id),
    captured_by UUID NOT NULL REFERENCES agents(id),
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL DEFAULT 'image/png',
    file_size BIGINT NOT NULL,
    storage_path TEXT NOT NULL,
    note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_screen_captures_channel ON screen_captures (channel_id, created_at DESC) WHERE deleted = FALSE;
```

### MongoDB — `screen_captures` collection
```kotlin
@Document(collection = "screen_captures")
data class ScreenCaptureDocument(
    @Id val id: String, val tenantId: String?, val channelId: String,
    val capturedBy: String, val originalFilename: String, val contentType: String,
    val fileSize: Long, val note: String?, val createdAt: Instant, val deleted: Boolean = false,
)
```

## 5. Hexagonal Architecture — File List

### Create (18 files)

| # | File | Role |
|---|------|------|
| 1 | `domain/ScreenCapture.kt` | Domain model |
| 2 | `port/inbound/ScreenCaptureUseCase.kt` | Command port (capture, download, delete) |
| 3 | `port/inbound/ScreenCaptureQuery.kt` | Query port (list, stream) |
| 4 | `port/outbound/ScreenCaptureRepository.kt` | R2DBC write port |
| 5 | `port/outbound/ScreenCaptureReadRepository.kt` | Mongo read port |
| 6 | `port/outbound/CaptureNotificationPort.kt` | SSE notification port |
| 7 | `application/ScreenCaptureService.kt` | Command service |
| 8 | `application/ScreenCaptureQueryService.kt` | Query service |
| 9 | `adapter/inbound/web/controller/ScreenCaptureController.kt` | REST controller |
| 10 | `adapter/inbound/web/dto/ScreenCaptureDtos.kt` | Response DTOs |
| 11 | `adapter/outbound/persistence/ScreenCaptureR2dbcRepository.kt` | PostgreSQL adapter |
| 12 | `adapter/outbound/persistence/ScreenCaptureDocument.kt` | Mongo document |
| 13 | `adapter/outbound/persistence/ScreenCaptureMongoRepository.kt` | Mongo adapter |
| 14 | `adapter/outbound/external/InMemoryCaptureNotificationAdapter.kt` | SSE adapter |
| 15 | `config/CaptureStorageProperties.kt` | Config properties |
| 16 | `resources/db/tenant-migration/V012__create_screen_captures.sql` | SQL migration |
| 17 | `test/.../application/ScreenCaptureServiceTest.kt` | Service unit tests |
| 18 | `test/.../application/ScreenCaptureQueryServiceTest.kt` | Query service tests |

All paths relative to `be/api/src/main/kotlin/com/counseling/api/` (except 16-18).

### Modify (2 files)

| File | Change |
|------|--------|
| `config/SecurityConfig.kt` | Add capture path matchers (GET permitAll, POST/DELETE authenticated) |
| `resources/application.yml` | Add `capture-storage` block |

## 6. CQRS Flow

```
POST capture → validate PNG/size → FileStoragePort.store()
  → ScreenCaptureRepository.save() (PostgreSQL)
  → CaptureNotificationPort.emitCapture() (SSE)
  → ScreenCaptureReadRepository.save() (MongoDB)

DELETE capture → softDelete PostgreSQL → markDeleted MongoDB → FileStoragePort.delete()
```

## 7. Configuration

```yaml
capture-storage:
  base-path: ${CAPTURE_STORAGE_BASE_PATH:/tmp/screen-captures}
  max-file-size: ${CAPTURE_STORAGE_MAX_SIZE:10485760}
```

## 8. Key Decisions

1. **Separate from SharedFile**: Different semantics (agent-only, PNG-only, optional note)
2. **Reuse FileStoragePort**: Same `LocalFileStorageAdapter` handles PNG storage
3. **Separate CaptureNotificationPort**: Different domain type than FileNotificationPort
4. **No frontend**: Agent app has no source files yet — frontend deferred

## 9. Build Sequence

**Phase 1 — Domain & Ports** (files 1-6): Pure interfaces, no dependencies
**Phase 2 — Services** (files 7-8): Implement use cases, depend on ports
**Phase 3 — Tests** (files 17-18): Unit tests with MockK + StepVerifier
**Phase 4 — Adapters** (files 9-16): Controllers, persistence, config, migration
**Phase 5 — Security** (modify SecurityConfig + application.yml)

Subtask split for parallel work:
- **Subtask A** (Phase 1+2+3): Domain, ports, services, tests — independent
- **Subtask B** (Phase 4+5): Adapters, controller, config — depends on A
