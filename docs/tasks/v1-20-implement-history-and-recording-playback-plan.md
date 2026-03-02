# v1-20: Implement History and Recording Playback

## 1. Overview

Counseling history view with filtering, search, cursor-based pagination, and secure recording streaming.

**CQRS Flow**: Channel close/recording stop/feedback submit → direct MongoDB projection → HistoryQuery reads from MongoDB.
**Auth**: COUNSELOR sees own history only; ADMIN sees all with optional filters.
**Dependencies**: Task 9 (Recording), 15 (Agent Dashboard), 17 (Admin Dashboard) — all done.

## 2. API Design

### GET /api/history — List history (cursor-paginated)
| Param | Type | Required | Description |
|-------|------|----------|-------------|
| agentId | UUID | No | Filter by agent (ADMIN only; COUNSELOR auto-filtered) |
| groupId | UUID | No | Filter by group |
| dateFrom | Instant | No | startedAt >= dateFrom |
| dateTo | Instant | No | startedAt <= dateTo |
| before | Instant | No | Cursor: endedAt < before |
| limit | Int | No (default 20) | Page size (1..100) |

**Response**: `{ items: HistoryItemResponse[], hasMore: boolean }`

### GET /api/history/{channelId} — Single history detail
**Response**: `HistoryDetailResponse` (includes recordings[], feedback, counselNotes[])

### GET /api/history/{channelId}/recording/{recordingId} — Stream recording
- Supports HTTP Range requests (206 Partial Content)
- Content-Type: video/mp4
- Auth: Bearer JWT (frontend uses blob URL approach for `<video>` element)

## 3. Data Model

### MongoDB: `channel_histories` collection
```
ChannelHistoryDocument:
  _id: String (channelId)
  tenantId, agentId, agentName, groupId, groupName: String?
  customerName, customerContact: String?
  status: String (WAITING|IN_PROGRESS|CLOSED)
  startedAt, endedAt: Instant?
  durationSeconds: Long?
  hasRecording: Boolean
  recordings: List<EmbeddedRecording>  // recordingId, status, startedAt, stoppedAt, durationSeconds
  feedbackRating: Int?, feedbackComment: String?
  counselNotes: List<EmbeddedCounselNote>  // id, content, createdAt
  createdAt, updatedAt: Instant
```
**Indexes**: `{tenantId:1, endedAt:-1}`, `{tenantId:1, agentId:1, endedAt:-1}`, `{tenantId:1, groupId:1, endedAt:-1}`

### No new PostgreSQL migrations needed
All source data exists in: channels, endpoints, recordings, feedbacks, counsel_notes tables.

## 4. Implementation File List

### Phase 1: Backend — Ports & Read Model (independent)
| File | Action | Role |
|------|--------|------|
| `port/inbound/HistoryQuery.kt` | CREATE | Query interface + result DTOs (HistoryFilter, HistoryListResult, HistoryDetail) |
| `port/inbound/RecordingStreamUseCase.kt` | CREATE | Recording file resource interface |
| `port/outbound/HistoryReadRepository.kt` | CREATE | MongoDB projection port (upsert, updateRecording, updateFeedback, updateStatus, find) |

### Phase 2: Backend — Persistence Adapter (depends on Phase 1)
| File | Action | Role |
|------|--------|------|
| `adapter/outbound/persistence/ChannelHistoryDocument.kt` | CREATE | MongoDB @Document with indexes |
| `adapter/outbound/persistence/HistoryMongoRepository.kt` | CREATE | ReactiveMongoTemplate implementation with dynamic Criteria |

### Phase 3: Backend — Application Services (depends on Phase 1-2)
| File | Action | Role |
|------|--------|------|
| `application/HistoryQueryService.kt` | CREATE | Implements HistoryQuery (cursor pagination, TenantContext) |
| `application/RecordingStreamService.kt` | CREATE | Implements RecordingStreamUseCase (file resolution, ownership check) |

### Phase 4: Backend — Projection Integration (depends on Phase 2)
| File | Action | Role |
|------|--------|------|
| `application/ChannelService.kt` | MODIFY | After closeChannel() → historyReadRepository.updateStatus() |
| `application/RecordingService.kt` | MODIFY | After stopRecording() → historyReadRepository.updateRecording() |
| `application/FeedbackService.kt` | MODIFY | After submit() → historyReadRepository.updateFeedback() |
| `application/QueueService.kt` | MODIFY | After channel assignment → historyReadRepository.upsert() initial document |

### Phase 5: Backend — Web Layer (depends on Phase 3)
| File | Action | Role |
|------|--------|------|
| `adapter/inbound/web/dto/HistoryDtos.kt` | CREATE | Request/Response DTOs |
| `adapter/inbound/web/controller/HistoryController.kt` | CREATE | REST controller (list, detail, stream recording) |

### Phase 6: Backend — Tests (depends on Phase 3-5)
| File | Action | Role |
|------|--------|------|
| `test/.../application/HistoryQueryServiceTest.kt` | CREATE | Kotest + MockK unit test |
| `test/.../adapter/inbound/web/controller/HistoryControllerTest.kt` | CREATE | Controller unit test |

### Phase 7: Frontend — History Page Upgrade (depends on Phase 5)
| File | Action | Role |
|------|--------|------|
| `fe/app-agent/src/hooks/use-history.ts` | REWRITE | useInfiniteQuery with cursor pagination + filters |
| `fe/app-agent/src/hooks/use-history-detail.ts` | CREATE | useQuery for single history detail |
| `fe/app-agent/src/components/history/history-filters.tsx` | CREATE | Date range, group select filter bar |
| `fe/app-agent/src/components/history/history-table.tsx` | CREATE | Table with IntersectionObserver infinite scroll |
| `fe/app-agent/src/components/history/recording-playback-modal.tsx` | CREATE | Modal with HTML5 video player |
| `fe/app-agent/src/app/history/page.tsx` | REWRITE | Compose filters + table + modal |

## 5. Key Decisions

1. **Direct projection** (not event listener): Matches FeedbackService pattern. Each service saves to MongoDB inline after command-side write, wrapped in `.onErrorResume`.
2. **Cursor pagination** on `endedAt DESC`: Consistent with NotificationQuery/ChatQuery patterns. Frontend uses `before` param from last item.
3. **Recording per recordingId**: URL `/api/history/{channelId}/recording/{recordingId}` since a channel can have multiple recordings.
4. **No new domain model**: History is a read-side view. HistoryProjection lives in port/outbound, Document in adapter.
5. **Auth at controller**: COUNSELOR forced to own agentId; ADMIN filters freely.

## 6. Build Sequence

1. **Phase 1-2** (ports + persistence): Can be implemented as one unit — defines contracts and MongoDB adapter
2. **Phase 3** (services): Query service + recording stream service
3. **Phase 4** (projections): Modify existing services to project into MongoDB — most critical integration point
4. **Phase 5** (web): Controller + DTOs
5. **Phase 6** (tests): Unit tests for services and controller
6. **Phase 7** (frontend): History page with infinite scroll + recording modal

Phases 1-5 are backend (can run in one worktree). Phase 7 is frontend (separate worktree).
