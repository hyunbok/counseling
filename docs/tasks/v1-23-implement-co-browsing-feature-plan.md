# v1-23: Implement Co-Browsing Feature

## 1. Overview

Co-browsing allows agents to view a customer's browser screen and guide them using pointer overlays and element highlighting.

- **Media path**: Customer shares browser tab via LiveKit screen share track (name: `cobrowse`)
- **Real-time interactions**: Pointer/highlight via LiveKit data channel (low latency, no backend)
- **Session state**: Backend manages lifecycle (REQUESTED → ACTIVE → ENDED) with CQRS + SSE
- **Dependencies**: Tasks 8 (LiveKit), 13 (customer call), 15 (agent dashboard)

## 2. API Design

Base: `POST/GET /api/channels/{channelId}/co-browsing`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/` | Agent JWT | Request co-browsing → creates REQUESTED session |
| POST | `/{sessionId}/start` | permitAll | Customer confirms screen share started → ACTIVE |
| POST | `/{sessionId}/end` | permitAll | Either party ends → ENDED |
| GET | `/active` | permitAll | Current active session |
| GET | `/` | Agent JWT | Session history (cursor pagination) |
| GET | `/stream` | permitAll | SSE stream of status changes |

**Response DTO**: `{ sessionId, channelId, initiatedBy, status, startedAt, endedAt, durationSeconds? }`

## 3. Data Model

### Domain: `CoBrowsingSession`
```
id: UUID, channelId: UUID, initiatedBy: UUID (agentId),
status: CoBrowsingStatus (REQUESTED|ACTIVE|ENDED),
startedAt: Instant?, endedAt: Instant?, createdAt: Instant, updatedAt: Instant, deleted: Boolean
```

### PostgreSQL (V013)
```sql
CREATE TABLE co_browsing_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel_id UUID NOT NULL REFERENCES channels(id),
    initiated_by UUID NOT NULL REFERENCES agents(id),
    status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    started_at TIMESTAMPTZ, ended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT ck_co_browsing_status CHECK (status IN ('REQUESTED','ACTIVE','ENDED'))
);
CREATE INDEX idx_co_browsing_channel ON co_browsing_sessions (channel_id, created_at DESC) WHERE NOT deleted;
CREATE UNIQUE INDEX idx_co_browsing_active ON co_browsing_sessions (channel_id) WHERE NOT deleted AND status IN ('REQUESTED','ACTIVE');
```

### MongoDB: `co_browsing_sessions` collection
Same fields as domain + `tenantId`. Compound index: `{tenantId:1, channelId:1, createdAt:-1}`.

## 4. Implementation Files

### Backend (create)
| File | Purpose |
|------|---------|
| `domain/CoBrowsingSession.kt` | Domain model with `start()`, `end()` state methods |
| `domain/CoBrowsingStatus.kt` | Enum: REQUESTED, ACTIVE, ENDED |
| `port/inbound/CoBrowsingUseCase.kt` | Command port: requestSession, startSession, endSession |
| `port/inbound/CoBrowsingQuery.kt` | Query port: getActive, listSessions, streamUpdates |
| `port/outbound/CoBrowsingSessionRepository.kt` | R2DBC write port |
| `port/outbound/CoBrowsingSessionReadRepository.kt` | MongoDB read port |
| `port/outbound/CoBrowsingNotificationPort.kt` | SSE port: emit/subscribe/removeChannel |
| `application/CoBrowsingService.kt` | Command service (CQRS write + SSE + projection) |
| `application/CoBrowsingQueryService.kt` | Query service (MongoDB reads + SSE subscribe) |
| `adapter/inbound/web/controller/CoBrowsingController.kt` | REST + SSE endpoints |
| `adapter/inbound/web/dto/CoBrowsingDtos.kt` | Request/response DTOs |
| `adapter/outbound/persistence/CoBrowsingSessionR2dbcRepository.kt` | PostgreSQL adapter |
| `adapter/outbound/persistence/CoBrowsingSessionDocument.kt` | MongoDB document |
| `adapter/outbound/persistence/CoBrowsingSessionMongoRepository.kt` | MongoDB adapter |
| `adapter/outbound/external/InMemoryCoBrowsingNotificationAdapter.kt` | SSE Sinks adapter |
| `db/tenant-migration/V013__create_co_browsing_sessions.sql` | DB migration |

### Backend (modify)
| File | Change |
|------|--------|
| `config/SecurityConfig.kt` | Add permitAll for GET active/stream, POST start/end |

### Backend (test)
| File | Purpose |
|------|---------|
| `application/CoBrowsingServiceTest.kt` | Kotest + MockK + StepVerifier |
| `application/CoBrowsingQueryServiceTest.kt` | Kotest + MockK + StepVerifier |

### Frontend Agent (create)
| File | Purpose |
|------|---------|
| `hooks/use-cobrowse.ts` | Session lifecycle (REST + SSE stream) |
| `hooks/use-cobrowse-data-channel.ts` | Send pointer/highlight via LiveKit data channel |
| `components/call/cobrowse-viewer.tsx` | View co-browse stream + mouse interaction overlay |
| `components/call/cobrowse-toolbar-button.tsx` | Toolbar button (Request/Waiting/Active states) |

### Frontend Agent (modify)
| File | Change |
|------|--------|
| `app/call/[id]/page.tsx` | Wire useCoBrowse hook, pass to VideoRoom + ToolBar |
| `components/call/video-room.tsx` | Render CoBrowseViewer when session active |
| `components/call/tool-bar.tsx` | Add CoBrowseToolbarButton |
| `stores/call-store.ts` | Add `coBrowsingSessionId` state |

### Frontend Customer (create)
| File | Purpose |
|------|---------|
| `hooks/use-cobrowse-session.ts` | SSE listener + getDisplayMedia + track publish |
| `hooks/use-cobrowse-data-channel.ts` | Receive pointer/highlight from data channel |
| `components/call/cobrowse-request-dialog.tsx` | Accept/decline modal |
| `components/call/cobrowse-pointer-overlay.tsx` | Agent's remote pointer cursor |
| `components/call/cobrowse-highlight-overlay.tsx` | Highlight rectangle overlay |

### Frontend Customer (modify)
| File | Change |
|------|--------|
| `app/call/[id]/page.tsx` | Add hooks + overlays + request dialog |

## 5. Data Channel Protocol

```typescript
// Agent → Customer (pointer, lossy, topic: 'cobrowse')
{ "type": "pointer", "x": 0.45, "y": 0.32 }
// Agent → Customer (highlight, reliable, topic: 'cobrowse')
{ "type": "highlight", "x": 0.1, "y": 0.2, "w": 0.3, "h": 0.15 }
// Agent → Customer (clear, reliable, topic: 'cobrowse')
{ "type": "clear_highlight" }
```

Coordinates normalized to 0-1 range (percentage of video dimensions). Pointer throttled to ~20fps.

## 6. Key Decisions

1. **LiveKit data channel for real-time interactions** — pointer/highlight at <100ms latency, not through backend
2. **Backend for session lifecycle** — CQRS audit trail, SSE for state sync between participants
3. **REQUESTED state via backend SSE** — robust request negotiation (survives reconnect)
4. **Track name `cobrowse`** — distinguishes co-browse from regular screen share
5. **Data channel topic `cobrowse`** — isolates messages from future data channel usage
6. **Unique partial index** — ensures max one active/requested session per channel

## 7. Build Sequence

```
Phase 1: BE Domain + Ports (no deps)
    ├→ Phase 2: BE Services + Tests (depends on Phase 1)
    └→ Phase 3: BE Adapters + DB Migration + Controller + Security (depends on Phase 1)
         ├→ Phase 4: FE Agent Hooks → Phase 5: FE Agent Components
         └→ Phase 6: FE Customer Hooks → Phase 7: FE Customer Components
```

- Phases 4-5 and 6-7 can run in parallel (agent vs customer frontend)
- Phase 2 and 3 can partially overlap (services need only ports, not adapters)
