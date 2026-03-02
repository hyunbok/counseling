# v1-18: Implement Notification System

## 1. Overview

Real-time in-app notifications via SSE and email notifications for critical events.

**Notification Types:**
| ID | Type | Recipient | Delivery | Persisted |
|----|------|-----------|----------|-----------|
| NTF-1 | NEW_COUNSELING_REQUEST | Agents (online, matching group) | IN_APP (SSE) | Yes |
| NTF-2 | QUEUE_POSITION_CHANGE | Customer | IN_APP (SSE) | No (already exists via QueueNotificationPort) |
| NTF-3 | CONNECTION_COMPLETE | Customer | IN_APP (SSE) | No (already exists via QueueNotificationPort) |
| NTF-4 | NEW_FEEDBACK | Agent (channel owner) | IN_APP (SSE) | Yes |
| NTF-5 | RECORDING_EXPIRY_WARNING | Company admin | EMAIL | Yes |
| NTF-6a | ACCOUNT_CREATED | Agent | EMAIL | Yes |
| NTF-6b | PASSWORD_RESET | Agent | EMAIL | Yes |

**Scope:** NTF-2 and NTF-3 are ephemeral and already handled by existing `QueueNotificationPort`. This task implements NTF-1, NTF-4, NTF-5, NTF-6a, NTF-6b.

**CQRS Flow:** Command (write) -> PostgreSQL via R2DBC -> Project to MongoDB (read) + Emit SSE via Reactor Sinks.

## 2. API Design

Base: `/api/notifications` (all endpoints require JWT auth)

| Method | Path | Description | Response |
|--------|------|-------------|----------|
| GET | `/stream` | SSE real-time stream | `Flux<NotificationSseEvent>` |
| GET | `/` | Paginated history | `NotificationListResponse` |
| GET | `/unread-count` | Unread count | `UnreadCountResponse` |
| PATCH | `/{id}/read` | Mark as read | `NotificationResponse` |
| PATCH | `/read-all` | Mark all as read | 204 |

Query params for GET `/`: `type`, `read`, `before` (cursor), `limit` (default 20, max 100).

## 3. Data Models

### PostgreSQL (Write Model) — V009__create_notifications.sql
```sql
CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id    UUID NOT NULL,
    recipient_type  VARCHAR(20) NOT NULL,
    type            VARCHAR(40) NOT NULL,
    title           VARCHAR(200) NOT NULL,
    body            TEXT NOT NULL,
    reference_id    UUID,
    reference_type  VARCHAR(30),
    delivery_method VARCHAR(10) NOT NULL DEFAULT 'IN_APP',
    read            BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_notifications_recipient_unread ON notifications (recipient_id, created_at DESC) WHERE deleted = FALSE AND read = FALSE;
CREATE INDEX idx_notifications_recipient_all ON notifications (recipient_id, created_at DESC) WHERE deleted = FALSE;
```

### MongoDB (Read Model) — `notifications` collection
Document: `{ id, recipientId, recipientType, type, title, body, referenceId, referenceType, read, createdAt, tenantId }`
Indexes: `{recipientId: 1, createdAt: -1}`, `{recipientId: 1, read: 1, createdAt: -1}`
Only IN_APP notifications are projected to MongoDB. `tenantId` is explicit (shared MongoDB across tenants).

### Domain Models
- `Notification(id, recipientId, recipientType, type, title, body, referenceId, referenceType, deliveryMethod, read, createdAt)`
- `NotificationType` enum: NEW_COUNSELING_REQUEST, NEW_FEEDBACK, RECORDING_EXPIRY_WARNING, ACCOUNT_CREATED, PASSWORD_RESET
- `RecipientType` enum: AGENT, ADMIN
- `DeliveryMethod` enum: IN_APP, EMAIL

## 4. Implementation Files

### Phase 1: Domain + Ports (no dependencies)
| File | Role |
|------|------|
| `domain/Notification.kt` | Domain model with `markAsRead()` |
| `domain/NotificationType.kt` | Enum |
| `domain/RecipientType.kt` | Enum |
| `domain/DeliveryMethod.kt` | Enum |
| `domain/event/NotificationEvent.kt` | Sealed class: Created, Read, AllRead |
| `port/inbound/NotificationUseCase.kt` | SendNotificationCommand + send/markAsRead/markAllAsRead |
| `port/inbound/NotificationQuery.kt` | getHistory/getUnreadCount/streamNotifications |
| `port/outbound/NotificationRepository.kt` | PostgreSQL write port |
| `port/outbound/NotificationReadRepository.kt` | MongoDB read port |
| `port/outbound/NotificationSsePort.kt` | emit/subscribe/removeRecipient |
| `port/outbound/EmailPort.kt` | send(EmailMessage) |

### Phase 2: Outbound Adapters
| File | Role |
|------|------|
| `db/tenant-migration/V009__create_notifications.sql` | PostgreSQL schema |
| `adapter/outbound/persistence/NotificationR2dbcRepository.kt` | R2DBC impl |
| `adapter/outbound/persistence/NotificationDocument.kt` | MongoDB document |
| `adapter/outbound/persistence/NotificationMongoRepository.kt` | MongoDB impl |
| `adapter/outbound/persistence/NotificationSseAdapter.kt` | Reactor Sinks impl |
| `adapter/outbound/external/EmailAdapter.kt` | JavaMailSender impl |
| `config/NotificationProperties.kt` | @ConfigurationProperties |

### Phase 3: Application Services
| File | Role |
|------|------|
| `application/NotificationService.kt` | Implements NotificationUseCase (CQRS write + SSE emit) |
| `application/NotificationQueryService.kt` | Implements NotificationQuery (MongoDB reads + SSE subscribe) |

### Phase 4: Inbound Adapter
| File | Role |
|------|------|
| `adapter/inbound/web/dto/NotificationDtos.kt` | DTOs |
| `adapter/inbound/web/controller/NotificationController.kt` | REST + SSE endpoints |

### Phase 5: Integration + Config
| File | Change |
|------|--------|
| `build.gradle.kts` | Add spring-boot-starter-mail |
| `application.yml` | Add spring.mail.* and notification.* config |
| `domain/Agent.kt` | Add `email: String?` field |
| `adapter/outbound/persistence/AgentR2dbcRepository.kt` | Add email column |
| `db/tenant-migration/V010__add_agent_email.sql` | ALTER TABLE agents ADD COLUMN email |
| `application/QueueService.kt` | Inject NotificationUseCase, trigger NTF-1 on queue enter |

### Phase 6: Tests
| File | Scope |
|------|-------|
| `test/NotificationServiceTest.kt` | Unit test (Kotest + MockK) |
| `test/NotificationQueryServiceTest.kt` | Unit test |
| `test/NotificationControllerTest.kt` | Controller test (SSE) |
| `test/NotificationSseAdapterTest.kt` | Sinks emit/subscribe test |

## 5. Key Decisions
- **Cursor-based pagination** (`before` timestamp) — consistent with ChatQuery, avoids offset shift on real-time inserts
- **No FK on recipient_id** — agents have UUIDs but recipients could be admins from different tables
- **tenantId in MongoDB only** — PostgreSQL uses per-tenant routing (TenantRoutingConnectionFactory)
- **Email via spring-boot-starter-mail** — simple JavaMailSender, non-blocking via Schedulers.boundedElastic()
- **No Thymeleaf** — simple HTML string templates to avoid unnecessary dependency; email templates are minimal

## 6. Build Sequence
1. Phase 1: Domain + Ports → Phase 2: Adapters → Phase 3: Services → Phase 4: Controller → Phase 5: Integration → Phase 6: Tests
2. Phase 1-2 can be parallelized (domain models and persistence adapters are independent)
3. Phase 3 depends on Phase 1-2
4. Phase 4 depends on Phase 3
5. Phase 5-6 depend on Phase 4
