# Task 9: Implement Recording with LiveKit Egress

## 1. Overview

LiveKit Egress를 사용하여 상담 세션 녹화(Room Composite → MP4). Agent가 채널 녹화를 시작/중지하고, 녹화 상태를 조회할 수 있다.

**Dependencies**: Task 8 (LiveKit WebRTC) — LiveKitPort, LiveKitAdapter, LiveKitConfig, LiveKitProperties, Channel.livekitRoomName 사용
**SDK**: `io.livekit:livekit-server:0.6.2` — `EgressServiceClient` (retrofit2 Call<T>, blocking)
**CQRS**: Command side only (PostgreSQL). MongoDB sync는 추후 이벤트 핸들러로 구현.

## 2. API Design

All endpoints require JWT auth. Only assigned agent or ADMIN can manage recordings.

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/channels/{channelId}/recordings` | Agent (assigned) | Start recording |
| POST | `/api/channels/{channelId}/recordings/stop` | Agent (assigned) | Stop recording |
| GET | `/api/channels/{channelId}/recordings` | Agent (assigned) | List recordings |

**POST /api/channels/{channelId}/recordings** → 201
```json
{ "recordingId": "uuid", "channelId": "uuid", "egressId": "EG_xxx", "status": "RECORDING", "startedAt": "..." }
```
Precondition: Channel IN_PROGRESS, no active recording.

**POST /api/channels/{channelId}/recordings/stop** → 200
```json
{ "recordingId": "uuid", "egressId": "EG_xxx", "status": "STOPPED", "startedAt": "...", "stoppedAt": "...", "filePath": "..." }
```

**GET /api/channels/{channelId}/recordings** → 200
```json
{ "recordings": [{ "recordingId": "uuid", "egressId": "EG_xxx", "status": "STOPPED", ... }] }
```

## 3. Data Model

### New: Recording domain
```kotlin
data class Recording(
    val id: UUID, val channelId: UUID, val egressId: String,
    val status: RecordingStatus, val filePath: String?,
    val startedAt: Instant, val stoppedAt: Instant?,
    val createdAt: Instant, val updatedAt: Instant, val deleted: Boolean = false,
)
enum class RecordingStatus { RECORDING, STOPPED, FAILED }
```

### DB Migration: V008__create_recordings.sql
```sql
CREATE TABLE recordings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id UUID NOT NULL REFERENCES channels(id),
    egress_id VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'RECORDING',
    file_path TEXT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    stopped_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_recordings_status CHECK (status IN ('RECORDING','STOPPED','FAILED'))
);
CREATE INDEX idx_recordings_channel ON recordings (channel_id) WHERE deleted = FALSE;
CREATE UNIQUE INDEX idx_recordings_active ON recordings (channel_id) WHERE deleted = FALSE AND status = 'RECORDING';
```
Unique partial index enforces one active recording per channel at DB level.

### Channel update
Add `withRecordingPath(path: String): Channel` method — sets `recordingPath` on stop.

## 4. Implementation Files

### Phase 1: Domain + Config + Migration
| File | Action | Description |
|------|--------|-------------|
| `domain/RecordingStatus.kt` | NEW | Enum |
| `domain/Recording.kt` | NEW | Domain model with `stop()`, `markFailed()`, `isActive()` |
| `domain/event/RecordingEvent.kt` | NEW | Started, Stopped, Failed sealed class |
| `config/RecordingProperties.kt` | NEW | `@ConfigurationProperties(prefix="recording")` — basePath, fileFormat |
| `config/LiveKitConfig.kt` | MODIFY | Add `egressServiceClient()` bean |
| `db/tenant-migration/V008__create_recordings.sql` | NEW | Recording table |
| `domain/Channel.kt` | MODIFY | Add `withRecordingPath()` |
| `application.yml` | MODIFY | Add recording defaults |
| `application-local.yml` | MODIFY | Add recording.base-path |
| `application-test.yml` | MODIFY | Add recording test config |

### Phase 2: Ports + Adapters
| File | Action | Description |
|------|--------|-------------|
| `port/outbound/LiveKitEgressPort.kt` | NEW | `startRoomCompositeEgress()`, `stopEgress()` |
| `port/outbound/RecordingRepository.kt` | NEW | CRUD + `findActiveByChannelId()` |
| `port/inbound/RecordingUseCase.kt` | NEW | Use case interface + result DTOs |
| `adapter/outbound/external/LiveKitEgressAdapter.kt` | NEW | Retrofit Call → Mono bridging |
| `adapter/outbound/persistence/RecordingR2dbcRepository.kt` | NEW | R2DBC implementation |

### Phase 3: Service + Controller
| File | Action | Description |
|------|--------|-------------|
| `application/RecordingService.kt` | NEW | Orchestrates recording lifecycle |
| `adapter/inbound/web/controller/RecordingController.kt` | NEW | REST endpoints |
| `adapter/inbound/web/dto/RecordingDtos.kt` | NEW | Request/Response DTOs |

### Phase 4: Tests
| File | Action | Description |
|------|--------|-------------|
| `test/.../RecordingServiceTest.kt` | NEW | Unit tests with mocked ports |
| `test/.../RecordingControllerTest.kt` | NEW | Controller integration tests |
| `test/.../domain/RecordingTest.kt` | NEW | Domain model tests |

## 5. Key Decisions

1. **Separate `recordings` table**: Channel can have multiple recording segments. `Channel.recordingPath` is denormalized shortcut to latest.
2. **Unique partial index**: DB-level enforcement of one active recording per channel.
3. **No request body for start/stop**: All config is server-side (basePath, format).
4. **`EgressServiceClient` blocking calls**: Wrap in `Mono.fromCallable { ... }.subscribeOn(Schedulers.boundedElastic())`.
5. **File path pattern**: `{basePath}/{tenantId}/{recordingId}.{format}` — stored on LiveKit Egress server's filesystem.
6. **Authorization**: Service-layer check — `channel.agentId == agentId` or agent.role == ADMIN.

## 6. Build Sequence

1. **Phase 1** (Domain/Config/Migration) → foundation, no dependencies
2. **Phase 2** (Ports/Adapters) → depends on Phase 1 domain models
3. **Phase 3** (Service/Controller) → depends on Phase 2 ports
4. **Phase 4** (Tests) → depends on Phase 3 service
