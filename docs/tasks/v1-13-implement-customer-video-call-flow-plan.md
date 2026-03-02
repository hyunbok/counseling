# v1-13: Implement Customer Video Call Flow

## 1. Overview

Complete the frontend customer video call flow in `fe/app-customer`. The backend APIs (queue, channel, chat, LiveKit) are already implemented. Frontend has stub pages and hooks that need to be connected to real APIs.

**CQRS Flow:** Customer enters queue (POST) → SSE position updates → Agent accepts → Customer gets channelId → Fetch LiveKit token → Connect to room → Chat during call → End call → Submit feedback.

**Critical Gap:** `PositionUpdate` domain event lacks `channelId`. When agent accepts (position=0), customer can't determine which channel to join. **Backend fix required.**

## 2. API Mapping (Backend → Frontend)

| Endpoint | Method | Auth | Request | Response |
|---|---|---|---|---|
| `/api/queue/enter` | POST | Public | `{name, contact, groupId?}` | `{entryId, position, queueSize}` |
| `/api/queue/{entryId}` | DELETE | Public | - | 204 |
| `/api/queue/position/{entryId}` | GET | Public | - | `{entryId, position, queueSize}` |
| `/api/queue/position/{entryId}/stream` | GET SSE | Public | - | `{entryId, position, queueSize, channelId?, timestamp}` |
| `/api/channels/{channelId}/customer-token` | GET | Public | `?name=` | `{token, roomName, identity, livekitUrl}` |
| `/api/channels/{channelId}/chat` | POST | Public | `{senderType, senderId, content}` | `{id, channelId, senderType, senderId, content, createdAt}` |
| `/api/channels/{channelId}/chat` | GET | Public | `?before=&limit=` | `{messages[], hasMore, oldestTimestamp}` |
| `/api/channels/{channelId}/chat/stream` | GET SSE | Public | - | SSE ChatMessageResponse |

**All endpoints require `X-Tenant-Id` header.**

## 3. Data Model Changes

### 3.1 Backend: Add channelId to PositionUpdate

`PositionUpdate.kt`: Add `val channelId: UUID? = null`
`PositionUpdateEvent` DTO: Add `val channelId: UUID? = null`
`QueueService.acceptCustomer`: Pass `channelId` when emitting position update
`QueueController.streamPosition`: Map channelId to DTO

### 3.2 Frontend: Update Zustand Store

```
CustomerState += {
  entryId: string | null;      // NEW - from queue/enter response
  setEntryId(id: string): void // NEW
}
```

Remove `channelId` from enter response; `channelId` comes from SSE position update when accepted.

## 4. Implementation Files

### Subtask 1: Backend — Add channelId to PositionUpdate (BE)
- `be/api/.../domain/PositionUpdate.kt` — add `channelId: UUID?`
- `be/api/.../dto/QueueDtos.kt` — add `channelId: UUID?` to `PositionUpdateEvent`
- `be/api/.../application/QueueService.kt` — pass channelId on accept
- `be/api/.../controller/QueueController.kt` — map channelId

### Subtask 2: API client + Store updates (FE)
- `fe/app-customer/src/lib/api.ts` — add `X-Tenant-Id` header (configurable)
- `fe/app-customer/src/stores/customer-store.ts` — add `entryId`, update `reset()`

### Subtask 3: Join page + Queue hook + Waiting page (FE)
- `fe/app-customer/src/app/page.tsx` — call enter API, store entryId, navigate
- `fe/app-customer/src/hooks/use-queue.ts` — rewrite: SSE for position, correct endpoints
- `fe/app-customer/src/app/waiting/page.tsx` — use SSE hook, handle accepted→navigate to /call

### Subtask 4: Video call page with LiveKit (FE)
- `fe/app-customer/src/hooks/use-video-call.ts` — DELETE (replaced by LiveKit components)
- `fe/app-customer/src/app/call/[id]/page.tsx` — LiveKitRoom + VideoConference + controls

### Subtask 5: In-call chat panel (FE)
- `fe/app-customer/src/hooks/use-chat.ts` — NEW: SSE messages + send mutation
- `fe/app-customer/src/components/chat/chat-panel.tsx` — NEW: message list + input

### Subtask 6: Feedback page (FE)
- `fe/app-customer/src/app/feedback/page.tsx` — wire API call (graceful fallback)

## 5. Key Decisions

1. **SSE over WebSocket for queue**: Backend already uses SSE (`TEXT_EVENT_STREAM_VALUE`). Use `EventSource` on frontend.
2. **LiveKit React components**: Use `@livekit/components-react` (`LiveKitRoom`, `VideoTrack`, `useParticipants`, `useLocalParticipant`) instead of raw SDK.
3. **Tenant ID**: Use `NEXT_PUBLIC_TENANT_ID` env var. Default to `"default"` for development.
4. **Chat SSE**: Use SSE stream for real-time messages (same pattern as queue position).

## 6. Build Sequence

| Phase | Subtask | Dependency | Agent |
|---|---|---|---|
| 1 | Backend: channelId in PositionUpdate | None | backend-developer |
| 2 | API client + Store updates | None | frontend-developer |
| 3 | Join + Queue + Waiting page | Phase 1, 2 | frontend-developer |
| 4 | Video call page (LiveKit) | Phase 2 | frontend-developer |
| 5 | Chat panel | Phase 4 | frontend-developer |
| 6 | Feedback page | Phase 2 | frontend-developer |

Phase 1 & 2 are independent → parallel. Phases 3-6 sequential (share component state).
