# v1-10: Implement Real-time Chat System

## 1. Overview

Real-time chat for in-call messaging within counseling channels.
- **Write path**: REST POST → ChatService → PostgreSQL (source of truth) → MongoDB (read projection) → SSE emit
- **Read path**: REST GET → ChatQueryService → MongoDB (cursor-based pagination)
- **Stream path**: SSE GET → ChatNotificationPort → Flux from Sinks.Many

## 2. API Design

### POST /api/channels/{channelId}/chat — Send Message
- **Auth**: permitAll (customers send without JWT; agents validated in service layer)
- **Request**: `{ "senderType": "AGENT"|"CUSTOMER", "senderId": "string", "content": "string" }`
- **Response** (201): `{ "id", "channelId", "senderType", "senderId", "content", "createdAt" }`
- **Errors**: 404 channel not found, 400 channel closed or validation failure

### GET /api/channels/{channelId}/chat — Message History
- **Auth**: permitAll
- **Query params**: `before: Instant?` (cursor), `limit: Int = 50` (1..100)
- **Response** (200): `{ "messages": [...], "hasMore": boolean, "oldestTimestamp": Instant? }`
- **Order**: ascending createdAt within page

### GET /api/channels/{channelId}/chat/stream — SSE Real-time Stream
- **Auth**: permitAll
- **Produces**: `text/event-stream`
- **Each event**: `ChatMessageResponse` JSON

## 3. Data Models

### PostgreSQL (Write) — Existing V005
```sql
chat_messages (id UUID PK, channel_id UUID FK, sender_type VARCHAR(20),
               sender_id VARCHAR(100), content TEXT, created_at TIMESTAMPTZ)
INDEX idx_chat_messages_channel (channel_id, created_at)
```

### MongoDB (Read) — Collection: `chat_messages`
```
{ _id: "uuid", channelId: "uuid", senderType: "AGENT", senderId: "...",
  content: "...", createdAt: ISODate }
Compound index: { channelId: 1, createdAt: -1 }
```

## 4. Implementation Files

### Phase 1: Ports & DTOs (no dependencies)
| File | Role |
|------|------|
| `port/inbound/ChatUseCase.kt` | Command interface: sendMessage |
| `port/inbound/ChatQuery.kt` | Query interface: getMessageHistory, streamMessages |
| `port/outbound/ChatMessageReadRepository.kt` | MongoDB read-side port |
| `port/outbound/ChatNotificationPort.kt` | SSE notification port |
| `adapter/inbound/web/dto/ChatDtos.kt` | Request/response DTOs |

### Phase 2: Adapters (depends on Phase 1)
| File | Role |
|------|------|
| `adapter/outbound/persistence/ChatMessageDocument.kt` | MongoDB document + domain mapping |
| `adapter/outbound/persistence/ChatMessageMongoRepository.kt` | MongoDB read adapter |
| `adapter/outbound/external/InMemoryChatNotificationAdapter.kt` | Sinks-based SSE adapter |

### Phase 3: Application Services (depends on Phase 1+2)
| File | Role |
|------|------|
| `application/ChatService.kt` | Command: send message (write PG → project Mongo → emit SSE) |
| `application/ChatQueryService.kt` | Query: history from MongoDB, stream from Sinks |

### Phase 4: Controller & Security (depends on Phase 3)
| File | Role |
|------|------|
| `adapter/inbound/web/controller/ChatController.kt` | REST + SSE endpoints |
| `config/SecurityConfig.kt` (MODIFY) | Add permitAll for chat endpoints |

### Modified Existing Files
| File | Change |
|------|--------|
| `port/outbound/ChatMessageRepository.kt` | Add findByChannelIdBefore method |
| `adapter/outbound/persistence/ChatMessageR2dbcRepository.kt` | Implement findByChannelIdBefore |

### Tests
| File | Role |
|------|------|
| `test/.../application/ChatServiceTest.kt` | Unit test: command flow with MockK |
| `test/.../adapter/outbound/external/InMemoryChatNotificationAdapterTest.kt` | Sinks emit/subscribe test |

## 5. Key Decisions
1. **SSE over WebSocket** — matches existing QueueNotificationPort pattern; no new infra needed
2. **Cursor-based pagination** — avoids shifting-window with append-only messages
3. **Synchronous MongoDB projection** — simpler than event listeners in reactive context
4. **Flat MongoDB documents** — unbounded messages per channel, individual docs with compound index
5. **No new Gradle dependencies** — spring-boot-starter-data-mongodb-reactive already present
6. **No new migrations** — V005 already has chat_messages table

## 6. Build Sequence
Phase 1 (parallel) → Phase 2 (parallel) → Phase 3 (sequential) → Phase 4 (sequential) → Tests → Build verify
