# v1-24: Network Reconnection Handling - Design Document

## 1. Overview

Implement robust network reconnection logic for WebRTC video calls with graceful degradation and Korean-language user feedback. This is a **frontend-only** task — LiveKit SDK handles reconnection at the transport layer; we configure its policy and build UI feedback.

**Scope:** Both `fe/app-customer` and `fe/app-agent` apps.

**Current Problem:**
- Neither app configures `RoomOptions.reconnectPolicy` — uses SDK defaults
- `onDisconnected` callbacks immediately navigate away (`/feedback` or `/dashboard`), preventing any reconnection
- No visual feedback during transient network disruptions

**CQRS Flow:** N/A — no backend changes required.

## 2. API Design

No new API endpoints. This feature uses LiveKit client SDK events only:
- `RoomEvent.Reconnecting` — fired when connection drops, SDK starts retrying
- `RoomEvent.SignalReconnecting` — signal connection specifically reconnecting
- `RoomEvent.Reconnected` — connection restored
- `RoomEvent.Disconnected` — permanent disconnect (all retries exhausted)

## 3. Data Model

No new domain models, DB schemas, or documents.

**Hook State:**
```typescript
type ConnectionStatus = 'connected' | 'reconnecting' | 'disconnected';
interface ReconnectionState {
  status: ConnectionStatus;
  retryCount: number;
  elapsedMs: number;
}
```

## 4. Implementation File List

### Subtask 1: Create `useReconnection` hook (both apps, parallel)
| Action | File |
|--------|------|
| CREATE | `fe/app-customer/src/hooks/use-reconnection.ts` |
| CREATE | `fe/app-agent/src/hooks/use-reconnection.ts` |

### Subtask 2: Create `ReconnectionOverlay` component (both apps, parallel)
| Action | File |
|--------|------|
| CREATE | `fe/app-customer/src/components/call/reconnection-overlay.tsx` |
| CREATE | `fe/app-agent/src/components/call/reconnection-overlay.tsx` |

### Subtask 3: Integrate into customer call page
| Action | File |
|--------|------|
| MODIFY | `fe/app-customer/src/app/call/[id]/page.tsx` |

Changes:
- Add `options` prop with `reconnectPolicy` to `<LiveKitRoom>`
- Use `useReconnection()` in `CallInner`, render `<ReconnectionOverlay>`
- Change `onDisconnected`: only navigate to `/feedback` on permanent disconnect

### Subtask 4: Integrate into agent call page
| Action | File |
|--------|------|
| MODIFY | `fe/app-agent/src/app/call/[id]/page.tsx` |

Changes:
- Add `options` prop with `reconnectPolicy` to `<LiveKitRoom>`
- Use `useReconnection()` in `CallPageInner`, render `<ReconnectionOverlay>`
- Enhance connection status indicator in top bar
- Pause elapsed timer during reconnection
- Change `onDisconnected`: only navigate to `/dashboard` on permanent disconnect

## 5. Key Decisions

1. **No shared package** — Both apps follow copy-paste convention (no `fe/packages/`). Adding a monorepo for 2 files is overkill.
2. **Custom overlay over `ConnectionStateToast`** — LiveKit's built-in toast is unstyled, English-only, no retry progress. We need Korean UI with elapsed timer.
3. **`DefaultReconnectPolicy` with extended delays** — `[300, 600, 1200, 2400, 4800, 8000, 10000, 10000, 10000, 10000]` (~57s total window). Counseling sessions are high-value; aggressive retry is preferred.
4. **Navigation driven by hook state** — `useEffect` watching `status === 'disconnected'` triggers navigation, not `onDisconnected` callback directly. This ensures consistent behavior.
5. **Overlay z-index: `z-50`** — Above video elements, below modals.

## 6. Build Sequence

```
Phase 1 (parallel):  Subtask 1 (hooks) + Subtask 2 (overlay components)
Phase 2 (sequential): Subtask 3 (customer integration) → Subtask 4 (agent integration)
```

Subtasks 1 & 2 are independent — can be built in parallel via worktrees.
Subtask 3 & 4 are independent of each other but depend on Phase 1.
