# v1-15: Implement Agent Dashboard and Call Interface - Design Plan

## 1. Overview

Wire the existing placeholder agent app (fe/app-agent, from Task 14) to real backend APIs. The agent app already has pages, components, hooks, and stores in place — this task makes them functional.

**Scope:** Frontend only (fe/app-agent)
**Dependencies:** Task 8 (LiveKit - done), Task 10 (Chat - done), Task 14 (Agent App Setup - done)
**CQRS Flow:** All reads go through REST/SSE endpoints; writes through POST/PUT endpoints.

## 2. API Endpoints (Backend - Already Implemented)

| Feature | Method | Endpoint | Auth |
|---------|--------|----------|------|
| Login | POST | /api/auth/login | No |
| Logout | POST | /api/auth/logout | JWT |
| Refresh | POST | /api/auth/refresh | No |
| Get Status | GET | /api/agents/me/status | JWT |
| Set Status | PUT | /api/agents/me/status | JWT |
| Queue List | GET | /api/queue | JWT |
| Queue SSE | GET | /api/queue/stream | No |
| Accept Customer | POST | /api/queue/{entryId}/accept | JWT |
| Channel Token | GET | /api/channels/{channelId}/token | JWT |
| Close Channel | POST | /api/channels/{channelId}/close | JWT |
| Channel List | GET | /api/channels?status= | JWT |
| Channel Detail | GET | /api/channels/{channelId} | No |
| Send Chat | POST | /api/channels/{channelId}/chat | No |
| Chat History | GET | /api/channels/{channelId}/chat?limit=50 | No |
| Chat Stream | GET | /api/channels/{channelId}/chat/stream | No |
| Start Recording | POST | /api/channels/{channelId}/recordings | JWT |
| Stop Recording | POST | /api/channels/{channelId}/recordings/stop | JWT |

## 3. Data Models (Frontend Types)

```typescript
// Agent & Auth
interface Agent { id: string; username: string; name: string; role: string; groupName?: string }
type AgentStatus = 'ONLINE' | 'AWAY' | 'WRAP_UP' | 'OFFLINE' | 'BUSY'

// Queue
interface QueueItem { entryId: string; name: string; contact: string; groupId?: string; enteredAt: string; waitDurationSeconds: number; position: number }
interface AcceptResponse { channelId: string; customerName: string; livekitUrl: string; agentToken: string }

// Channel
type ChannelStatus = 'WAITING' | 'IN_PROGRESS' | 'CLOSED'
interface ChannelSummary { id: string; status: ChannelStatus; customerName?: string; startedAt?: string; endedAt?: string }
interface ChannelToken { token: string; roomName: string; livekitUrl: string }

// Chat
type SenderType = 'AGENT' | 'CUSTOMER'
interface ChatMessage { id: string; channelId: string; senderType: SenderType; senderId: string; content: string; createdAt: string }

// Queue SSE
interface QueueUpdateEvent { type: 'ENTERED'|'LEFT'|'ACCEPTED'|'QUEUE_CHANGED'; entryId?: string; customerName?: string; queueSize: number; timestamp: string }
```

## 4. Implementation File List by Subtask

### ST-1: Real-time Queue with SSE (new hook + update queue-list)
- **NEW** `src/hooks/use-queue-stream.ts` — SSE hook for GET /api/queue/stream
- **MODIFY** `src/hooks/use-queue.ts` — Update accept response types
- **MODIFY** `src/components/queue/queue-list.tsx` — Use SSE for updates, update accept flow to store agentToken
- **MODIFY** `src/stores/call-store.ts` — Add agentToken, livekitUrl fields

### ST-2: LiveKit Video Call Integration (replace placeholder)
- **MODIFY** `src/hooks/use-video-call.ts` — Replace stubs with real LiveKit Room/hooks
- **MODIFY** `src/components/call/video-room.tsx` — Use LiveKitRoom + VideoTrack + AudioTrack components
- **MODIFY** `src/components/call/tool-bar.tsx` — Wire mic/camera/screenshare to LiveKit controls
- **MODIFY** `src/app/call/[id]/page.tsx` — Fetch token from API, pass to LiveKit, handle disconnect

### ST-3: In-Call Chat Panel with SSE (replace hardcoded)
- **NEW** `src/hooks/use-chat.ts` — SSE chat stream + history + send (mirror customer app pattern)
- **MODIFY** `src/components/call/chat-panel.tsx` — Wire to real chat hook

### ST-4: In-Call Note Panel (wire to API or local)
- **NEW** `src/hooks/use-notes.ts` — Auto-save notes to backend or localStorage
- **MODIFY** `src/components/call/note-panel.tsx` — Wire to notes hook with auto-save

### ST-5: Counseling History Page (wire to API)
- **NEW** `src/hooks/use-history.ts` — GET /api/channels with filters
- **MODIFY** `src/app/history/page.tsx` — Replace mock data with real API, add pagination

### ST-6: Recording Controls (wire to API)
- **NEW** `src/hooks/use-recording.ts` — Start/stop recording API calls
- **MODIFY** `src/components/call/tool-bar.tsx` — Wire recording button
- **MODIFY** `src/app/call/[id]/page.tsx` — Auto-start recording on call join

## 5. Key Design Decisions

1. **SSE for Queue (not polling):** Use GET /api/queue/stream SSE endpoint to get real-time queue updates instead of 5-second polling. Fall back to polling if SSE disconnects.

2. **LiveKit Components:** Use `@livekit/components-react` (already installed) for `LiveKitRoom`, `VideoTrack`, `useLocalParticipant`, `useRemoteParticipants`, `useTrackToggle`.

3. **Chat SSE Pattern:** Follow the exact pattern from fe/app-customer's use-chat.ts — fetch history on mount, then subscribe to SSE stream, merge messages by ID dedup.

4. **Notes:** Since there's no dedicated notes API endpoint yet, save notes to localStorage keyed by channelId, with an optional POST to a future endpoint. This allows agents to take notes now without backend dependency.

5. **Token Flow on Accept:** When agent accepts a customer, POST /api/queue/{id}/accept returns the agentToken and channelId. Store both in call-store, then navigate to /call/{channelId}. The call page reads the token from store instead of fetching separately.

6. **Recording Auto-Start:** Auto-start recording when the call connects. Show REC badge. Agent can manually stop via toolbar.

## 6. Build Sequence

| Phase | Subtask | Dependencies | Parallel? |
|-------|---------|-------------|-----------|
| 1 | ST-1: Queue SSE + Accept flow | None | Yes |
| 1 | ST-3: Chat hook | None | Yes |
| 1 | ST-5: History hook + page | None | Yes |
| 2 | ST-2: LiveKit Video Call | ST-1 (needs token in store) | No |
| 2 | ST-4: Note Panel | None | Yes with ST-2 |
| 3 | ST-6: Recording Controls | ST-2 (needs LiveKit connected) | No |

**Phase 1** (parallel): ST-1, ST-3, ST-5 — independent hooks and API wiring
**Phase 2** (after Phase 1): ST-2 + ST-4 — video call needs accept flow from ST-1
**Phase 3** (after Phase 2): ST-6 — recording needs video call working
