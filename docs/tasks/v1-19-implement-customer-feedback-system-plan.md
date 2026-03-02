# v1-19: Implement Customer Feedback System — Design

## Overview
Post-call feedback collection (rating 1-5, optional comment) linked to channels.
CQRS: PostgreSQL write-side (existing) → inline projection to MongoDB read-side (new).
Notification: Agent receives `NEW_FEEDBACK` notification via existing `NotificationUseCase`.

## Existing Assets (No Changes Needed)
- `domain/Feedback.kt` — domain model with `require(rating in 1..5)`
- `domain/event/FeedbackEvent.kt` — `FeedbackEvent.Submitted` sealed class
- `domain/NotificationType.kt` — `NEW_FEEDBACK` already present
- `port/outbound/FeedbackRepository.kt` — `save`, `findByChannelId`
- `adapter/outbound/persistence/FeedbackR2dbcRepository.kt` — `ON CONFLICT (channel_id) DO NOTHING`
- `V006__create_counsel_notes_and_feedback.sql` — `UNIQUE(channel_id)`, `CHECK(rating BETWEEN 1 AND 5)`

## API Design

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/channels/{channelId}/feedback` | permitAll (customer) | Submit feedback |
| GET | `/api/channels/{channelId}/feedback` | authenticated (agent) | Get feedback for channel |
| GET | `/api/feedback/stats` | authenticated (agent) | Aggregated statistics |

### POST Request/Response
Request: `{ "rating": 4, "comment": "Great service" }`
Response (201): `{ "id", "channelId", "rating", "comment", "createdAt" }`
Errors: 404 (channel not found), 409 (duplicate — handled by ON CONFLICT)

### GET Stats Query Params
`?from=...&to=...` (Instant, optional — defaults to last 30 days)
Response: `{ "totalCount", "averageRating", "distribution": {1:N,2:N,...}, "from", "to" }`

## MongoDB Read-Side
Collection: `feedbacks`
Indexes: `{tenantId:1,channelId:1}` (unique), `{tenantId:1,createdAt:-1}`
Stats via aggregation pipeline ($match → $group).

## CQRS Flow
```
POST /api/channels/{channelId}/feedback
  → FeedbackController → FeedbackUseCase.submit()
    → FeedbackService:
      1. TenantContext.getTenantId()
      2. channelRepository.findByIdAndNotDeleted() — validate
      3. feedbackRepository.save() — PostgreSQL write
      4. feedbackReadRepository.save() — MongoDB projection (.onErrorResume)
      5. notificationUseCase.send(NEW_FEEDBACK) — fire-and-forget .subscribe()
```

## Implementation Files

### Phase 1: Ports (pure interfaces)
1. CREATE `port/inbound/FeedbackUseCase.kt` — `SubmitFeedbackCommand` + interface
2. CREATE `port/inbound/FeedbackQuery.kt` — `FeedbackStatsResult` + interface
3. CREATE `port/outbound/FeedbackReadRepository.kt` — MongoDB read port

### Phase 2: Outbound Adapters
4. CREATE `adapter/outbound/persistence/FeedbackDocument.kt` — MongoDB document
5. CREATE `adapter/outbound/persistence/FeedbackMongoRepository.kt` — read-side impl

### Phase 3: Application Services
6. CREATE `application/FeedbackService.kt` — implements FeedbackUseCase
7. CREATE `application/FeedbackQueryService.kt` — implements FeedbackQuery

### Phase 4: Inbound Adapters
8. CREATE `adapter/inbound/web/dto/FeedbackDtos.kt` — request/response DTOs
9. CREATE `adapter/inbound/web/controller/FeedbackController.kt` — REST endpoints
10. MODIFY `config/SecurityConfig.kt` — add `POST /api/channels/*/feedback` permitAll

### Phase 5: Tests
11. CREATE `test/.../application/FeedbackServiceTest.kt`
12. CREATE `test/.../application/FeedbackQueryServiceTest.kt`

### Phase 6: Build verification
13. `./gradlew build` — compile + all tests pass

## Key Decisions
- **Inline CQRS sync** (not event-listener) — matches NotificationService pattern
- **On-the-fly aggregation** — no pre-computed stats collection needed at this scale
- **One feedback per channel** — DB UNIQUE + ON CONFLICT handles idempotency
- **Fire-and-forget notification** — `.subscribe()` in `doOnNext`
