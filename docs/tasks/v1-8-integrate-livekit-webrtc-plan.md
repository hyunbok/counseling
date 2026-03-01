# v1-8: Integrate LiveKit for WebRTC — Implementation Plan

## 1. Overview

LiveKit SDK integration for 1:1 video counseling. Manages room lifecycle (create/delete), JWT token generation for participants, and channel close orchestration.

**Existing infrastructure (already in place):**
- `io.livekit:livekit-server:0.6.2` dependency in `build.gradle.kts`
- `livekit:` config block in `application-local.yml` (url, api-key, api-secret)
- `Channel` domain model with `WAITING → IN_PROGRESS → CLOSED` state machine
- `Endpoint` domain model for channel participants
- `ChannelRepository`, `EndpointRepository` ports + R2DBC adapters
- `QueueService.acceptCustomer()` creates Channel + Endpoints (no LiveKit room yet)

**Key design decisions:**
1. LiveKit SDK uses synchronous `retrofit2.Call<T>` — bridge via `Mono.fromCallable { call.execute() }.subscribeOn(Schedulers.boundedElastic())`
2. Token generation (`AccessToken.toJwt()`) is pure computation — runs on event loop directly
3. Room naming: `{tenantId}-channel-{channelId}` for tenant isolation
4. New outbound port `LiveKitPort` abstracts SDK completely from domain/application layers
5. `ChannelService` orchestrates full call lifecycle as a new `ChannelUseCase`

## 2. API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/channels/{id}/token` | JWT (agent) | Agent LiveKit token |
| GET | `/api/channels/{id}/customer-token?name={name}` | Public | Customer LiveKit token |
| POST | `/api/channels/{id}/close` | JWT (agent, owner) | Close channel + delete room |
| GET | `/api/channels/{id}` | JWT | Channel detail |
| GET | `/api/channels?status={status}` | JWT | List agent's channels |

**Token Response (200):**
```json
{ "token": "eyJ...", "roomName": "tenant1-channel-uuid", "identity": "agent:uuid", "livekitUrl": "wss://..." }
```

**Error codes:** 401 (unauth), 403 (not owner), 404 (not found), 409 (invalid status)

## 3. Data Model Changes

### Channel domain — add `livekitRoomName`
```kotlin
data class Channel(..., val livekitRoomName: String? = null) {
    fun withRoomName(name: String): Channel = copy(livekitRoomName = name, updatedAt = Instant.now())
}
```

### DB Migration — V007
```sql
ALTER TABLE channels ADD COLUMN livekit_room_name VARCHAR(200);
CREATE INDEX idx_channels_livekit_room ON channels (livekit_room_name) WHERE livekit_room_name IS NOT NULL;
```

### New domain event — `ChannelEvent.RoomCreated`

## 4. Implementation File List

### New Files (9)

| # | File | Layer | Subtask |
|---|------|-------|---------|
| 1 | `config/LiveKitProperties.kt` | Config | ST-1 |
| 2 | `config/LiveKitConfig.kt` | Config | ST-1 |
| 3 | `port/outbound/LiveKitPort.kt` | Port | ST-2,3 |
| 4 | `adapter/outbound/external/LiveKitAdapter.kt` | Adapter | ST-2,3 |
| 5 | `port/inbound/ChannelUseCase.kt` | Port | ST-4 |
| 6 | `application/ChannelService.kt` | Application | ST-4 |
| 7 | `adapter/inbound/web/controller/ChannelController.kt` | Web | ST-5 |
| 8 | `adapter/inbound/web/dto/ChannelDtos.kt` | Web | ST-5 |
| 9 | `db/tenant-migration/V007__add_livekit_room_to_channels.sql` | DB | ST-1 |

### Modified Files (7)

| File | Change | Subtask |
|------|--------|---------|
| `domain/Channel.kt` | Add `livekitRoomName` field + `withRoomName()` | ST-1 |
| `domain/event/ChannelEvent.kt` | Add `RoomCreated` event | ST-4 |
| `port/outbound/ChannelRepository.kt` | Add `findAllByAgentIdAndStatusAndNotDeleted()` | ST-4 |
| `adapter/outbound/persistence/ChannelR2dbcRepository.kt` | Handle `livekit_room_name` in SQL | ST-2 |
| `application/QueueService.kt` | Delegate room creation to ChannelService | ST-4 |
| `config/SecurityConfig.kt` | Permit customer-token endpoint | ST-5 |
| `application.yml` / `application-test.yml` | Add livekit config section | ST-1 |

### Test Files (3)

| File | Scope |
|------|-------|
| `test/.../application/ChannelServiceTest.kt` | Unit test (MockK) |
| `test/.../adapter/outbound/external/LiveKitAdapterTest.kt` | Unit test (MockK) |
| `test/.../adapter/inbound/web/controller/ChannelControllerTest.kt` | WebFlux test |

## 5. CQRS Flow

### Accept Customer (modified existing flow)
```
QueueService.acceptCustomer()
  → Create Channel (WAITING → IN_PROGRESS)
  → Create Endpoints (agent + customer)
  → LiveKitPort.createRoom(tenantId-channel-{id})    ← NEW
  → channel.withRoomName(roomName)                     ← NEW
  → ChannelRepository.save(channel)                    ← NEW
  → LiveKitPort.generateToken() × 2                   ← NEW
  → Publish ChannelEvent.RoomCreated                   ← NEW
  → Return AcceptResult (+ roomName, tokens)
```

### Close Channel (new)
```
ChannelService.closeChannel(channelId, agentId)
  → ChannelRepository.findById() → validate owner + isOpen
  → LiveKitPort.deleteRoom(livekitRoomName)
  → Update Endpoints: leftAt = now
  → channel.close() → ChannelRepository.save()
  → Publish ChannelEvent.Closed
```

### Token Generation (new, stateless)
```
ChannelService.getToken(channelId, identity)
  → ChannelRepository.findById() → validate isOpen
  → LiveKitPort.generateToken(roomName, identity, ...)
  → Return TokenResult
```

## 6. Build Sequence

### Phase 1: Foundation (parallelizable — ST-1)
- `LiveKitProperties.kt`, `LiveKitConfig.kt`
- `V007__add_livekit_room_to_channels.sql`
- Modify `Channel.kt` (add `livekitRoomName`)
- Modify `ChannelR2dbcRepository.kt` (handle new column)
- Update `application.yml` / `application-test.yml`

### Phase 2: LiveKit Port + Adapter (ST-2, ST-3 — parallelizable after Phase 1)
- `LiveKitPort.kt` (outbound port interface)
- `LiveKitAdapter.kt` (Retrofit Call → Mono bridge + token generation)
- `LiveKitAdapterTest.kt`

### Phase 3: Channel Use Case + Service (ST-4 — after Phase 2)
- `ChannelUseCase.kt` (inbound port + result DTOs)
- `ChannelService.kt` (implements ChannelUseCase)
- Modify `QueueService.kt` (integrate LiveKit room creation)
- Modify `ChannelEvent.kt` (add RoomCreated)
- Modify `ChannelRepository.kt` (add query method)
- `ChannelServiceTest.kt`

### Phase 4: REST API (ST-5 — after Phase 3)
- `ChannelDtos.kt`
- `ChannelController.kt`
- Modify `SecurityConfig.kt`
- `ChannelControllerTest.kt`

### Phase 5: Build Verification
- `./gradlew ktlintCheck`
- `./gradlew test`
- `./gradlew build`

## 7. Security

| Endpoint | Auth | Permission |
|----------|------|------------|
| `GET .../token` | JWT required | `channel.agentId == principal` |
| `GET .../customer-token` | Public | UUID unguessable + name matches endpoint |
| `POST .../close` | JWT required | `channel.agentId == principal` |
| `GET .../` | JWT required | Returns only agent's own channels |

Customer token security relies on: (1) UUID channel ID is unguessable, (2) customer name must match an existing endpoint, (3) channel must be IN_PROGRESS.
