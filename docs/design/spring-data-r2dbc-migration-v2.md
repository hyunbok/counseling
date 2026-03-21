# Spring Data R2DBC Migration v2: ReactiveCrudRepository

## 1. Architecture Overview

### Current State
All R2dbc adapter classes use `R2dbcEntityTemplate` for reads and `DatabaseClient` raw SQL for writes (upserts). This is verbose and bypasses Spring Data's query derivation.

### Target State
Introduce `ReactiveCrudRepository` interfaces for all standard CRUD and query derivation. Keep `DatabaseClient` raw SQL **only** for operations that cannot be expressed via Spring Data:
- `ON CONFLICT ... DO UPDATE` (upsert)
- `ON CONFLICT ... DO NOTHING` (insert-ignore)
- `@Modifying @Query` for bulk updates (markAsRead, softDelete)
- Dynamic search with optional filter parameters

### Adapter Pattern
Each existing adapter class (e.g., `AgentR2dbcRepository`) implements a **port interface** (e.g., `AgentRepository`). It will now be composed of:
1. A **Spring Data interface** (e.g., `SpringDataAgentRepository extends ReactiveCrudRepository`) for derived queries
2. A **DatabaseClient** for upsert/raw SQL (injected via the template's `databaseClient`)

The adapter class delegates to both.

```
Port Interface (domain layer)
       ^
       |  implements
Adapter Class (@Repository)
       |         |
       v         v
SpringData    DatabaseClient
Interface     (raw SQL for upserts)
```

---

## 2. Package Structure

### api module
```
com.counseling.api.adapter.outbound.persistence/
  entity/                           # @Table entity classes (MODIFY: add Persistable)
    AgentEntity.kt
    ChannelEntity.kt
    ...
  springdata/                       # NEW: Spring Data interfaces
    SpringDataAgentRepository.kt
    SpringDataChannelRepository.kt
    SpringDataChatMessageRepository.kt
    SpringDataCoBrowsingSessionRepository.kt
    SpringDataCompanyRepository.kt
    SpringDataCounselNoteRepository.kt
    SpringDataEndpointRepository.kt
    SpringDataFeedbackRepository.kt
    SpringDataGroupRepository.kt
    SpringDataNotificationRepository.kt
    SpringDataRecordingRepository.kt
    SpringDataScreenCaptureRepository.kt
    SpringDataSharedFileRepository.kt
    SpringDataTenantRepository.kt
  AgentR2dbcRepository.kt           # MODIFY: delegate to SpringData + DatabaseClient
  ChannelR2dbcRepository.kt
  ...
```

### api-admin module
```
com.counseling.admin.adapter.outbound.persistence/
  entity/                           # Same as current
  springdata/                       # NEW: Spring Data interfaces
    AdminSpringDataAgentRepository.kt
    AdminSpringDataChannelRepository.kt
    AdminSpringDataCompanyRepository.kt
    AdminSpringDataGroupRepository.kt
    AdminSpringDataSuperAdminRepository.kt
    AdminSpringDataTenantRepository.kt
  AdminAgentR2dbcRepository.kt      # MODIFY: delegate to SpringData + DatabaseClient
  ...
```

---

## 3. Multi-Tenant R2dbcRepositories Configuration

### Problem
- **Tenant DB** (routing): Most repositories (Agent, Channel, Group, etc.)
- **Meta DB** (fixed): Tenant, SuperAdmin repositories

Spring Data R2DBC's `@EnableR2dbcRepositories` scans packages and binds to a single `R2dbcEntityOperations` bean. We need TWO configurations pointing to different `ConnectionFactory`/`R2dbcEntityOperations` beans.

### Solution: Separate packages + `entityOperationsRef`

#### api module - R2dbcConfig.kt changes

```kotlin
@Configuration
@Profile("!test")
@EnableR2dbcRepositories(
    basePackages = ["com.counseling.api.adapter.outbound.persistence.springdata"],
    entityOperationsRef = "tenantR2dbcEntityTemplate",
    // Exclude meta-DB repos (Tenant) via explicit exclusion or separate package
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [SpringDataTenantRepository::class]
        )
    ]
)
class R2dbcConfig {
    // ... existing beans ...
}

@Configuration
@Profile("!test")
@EnableR2dbcRepositories(
    basePackages = ["com.counseling.api.adapter.outbound.persistence.springdata"],
    entityOperationsRef = "metaR2dbcEntityTemplate",
    includeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [SpringDataTenantRepository::class]
        )
    ]
)
class MetaR2dbcConfig
```

**Alternative (simpler, recommended):** Split into sub-packages:

```
springdata/
  tenant/    <-- scanned with tenantR2dbcEntityTemplate
    SpringDataAgentRepository.kt
    SpringDataChannelRepository.kt
    ...
  meta/      <-- scanned with metaR2dbcEntityTemplate
    SpringDataTenantRepository.kt
```

```kotlin
@Configuration
@Profile("!test")
@EnableR2dbcRepositories(
    basePackages = ["com.counseling.api.adapter.outbound.persistence.springdata.tenant"],
    entityOperationsRef = "tenantR2dbcEntityTemplate"
)
class TenantR2dbcRepositoryConfig

@Configuration
@Profile("!test")
@EnableR2dbcRepositories(
    basePackages = ["com.counseling.api.adapter.outbound.persistence.springdata.meta"],
    entityOperationsRef = "metaR2dbcEntityTemplate"
)
class MetaR2dbcRepositoryConfig
```

#### api-admin module - R2dbcConfig.kt changes

Same pattern:

```kotlin
@Configuration
@Profile("!test")
@EnableR2dbcRepositories(
    basePackages = ["com.counseling.admin.adapter.outbound.persistence.springdata.tenant"],
    entityOperationsRef = "tenantR2dbcEntityTemplate"
)
class TenantR2dbcRepositoryConfig

@Configuration
@Profile("!test")
@EnableR2dbcRepositories(
    basePackages = ["com.counseling.admin.adapter.outbound.persistence.springdata.meta"],
    entityOperationsRef = "metaR2dbcEntityTemplate"
)
class MetaR2dbcRepositoryConfig
```

**Meta-DB repos in api-admin:** `AdminSpringDataTenantRepository`, `AdminSpringDataSuperAdminRepository`
**Tenant-DB repos in api-admin:** `AdminSpringDataAgentRepository`, `AdminSpringDataChannelRepository`, `AdminSpringDataCompanyRepository`, `AdminSpringDataGroupRepository`

---

## 4. Entity Class Changes

### Problem: `save()` INSERT vs UPDATE detection

Spring Data R2DBC uses `isNew()` to decide INSERT vs UPDATE:
- `@Id` field is `null` or `0` -> INSERT
- `@Id` field is non-null -> UPDATE

Our domain creates entities with **pre-set UUIDs**, so `id` is always non-null. This means `save()` would always attempt UPDATE, which fails for new entities.

### Solution: Do NOT use `Persistable<ID>`

Since ALL save methods currently use `ON CONFLICT (id) DO UPDATE SET ...` (true upsert), and Spring Data's `save()` cannot do upsert, **all save methods will continue using `DatabaseClient` raw SQL**. The Spring Data interfaces are used **only for query methods**.

This means:
- **No changes to entity classes** -- they stay as-is
- **No `Persistable<ID>` implementation needed**
- Entity classes continue to serve as mapping targets for Spring Data query results

### Enum Handling

Spring Data R2DBC converts enums to/from strings by default. Our entities already use enum types (e.g., `ChannelStatus`, `AgentRole`). The `@Table` entity mapping handles this automatically via Spring Data's default converters. No changes needed.

---

## 5. Spring Data Interface Definitions

### 5.1 api module -- Tenant DB (`springdata/tenant/`)

#### SpringDataAgentRepository

```kotlin
interface SpringDataAgentRepository :
    ReactiveCrudRepository<AgentEntity, UUID> {

    fun findByUsernameAndDeletedFalse(username: String): Mono<AgentEntity>

    fun findByIdAndDeletedFalse(id: UUID): Mono<AgentEntity>

    fun findAllByGroupIdAndDeletedFalse(groupId: UUID): Flux<AgentEntity>

    fun findAllByDeletedFalse(): Flux<AgentEntity>
}
```

#### SpringDataChannelRepository

```kotlin
interface SpringDataChannelRepository :
    ReactiveCrudRepository<ChannelEntity, UUID> {

    fun findByIdAndDeletedFalse(id: UUID): Mono<ChannelEntity>

    fun findAllByAgentIdAndDeletedFalseOrderByCreatedAt(agentId: UUID): Flux<ChannelEntity>

    fun findAllByStatusAndDeletedFalseOrderByCreatedAt(status: ChannelStatus): Flux<ChannelEntity>

    fun findAllByAgentIdAndStatusAndDeletedFalseOrderByCreatedAt(
        agentId: UUID,
        status: ChannelStatus,
    ): Flux<ChannelEntity>

    fun findByLivekitRoomNameAndDeletedFalse(livekitRoomName: String): Mono<ChannelEntity>
}
```

#### SpringDataChatMessageRepository

```kotlin
interface SpringDataChatMessageRepository :
    ReactiveCrudRepository<ChatMessageEntity, UUID> {

    fun findAllByChannelIdOrderByCreatedAt(channelId: UUID): Flux<ChatMessageEntity>

    fun findAllByChannelIdAndCreatedAtLessThanOrderByCreatedAtDesc(
        channelId: UUID,
        createdAt: Instant,
        pageable: Pageable,
    ): Flux<ChatMessageEntity>
}
```

Note: `limit` parameter uses `Pageable.ofSize(limit)` at the call site.

#### SpringDataCoBrowsingSessionRepository

```kotlin
interface SpringDataCoBrowsingSessionRepository :
    ReactiveCrudRepository<CoBrowsingSessionEntity, UUID> {

    fun findByIdAndDeletedFalse(id: UUID): Mono<CoBrowsingSessionEntity>

    fun findByChannelIdAndDeletedFalseAndStatusIn(
        channelId: UUID,
        status: Collection<CoBrowsingStatus>,
    ): Mono<CoBrowsingSessionEntity>
}
```

#### SpringDataCompanyRepository

```kotlin
interface SpringDataCompanyRepository :
    ReactiveCrudRepository<CompanyEntity, UUID> {

    fun findFirstBy(): Mono<CompanyEntity>
}
```

#### SpringDataCounselNoteRepository

```kotlin
interface SpringDataCounselNoteRepository :
    ReactiveCrudRepository<CounselNoteEntity, UUID> {

    fun findByIdAndDeletedFalse(id: UUID): Mono<CounselNoteEntity>

    fun findAllByChannelIdAndDeletedFalseOrderByCreatedAt(channelId: UUID): Flux<CounselNoteEntity>
}
```

#### SpringDataEndpointRepository

```kotlin
interface SpringDataEndpointRepository :
    ReactiveCrudRepository<EndpointEntity, UUID> {

    fun findAllByChannelIdOrderByJoinedAt(channelId: UUID): Flux<EndpointEntity>
}
```

#### SpringDataFeedbackRepository

```kotlin
interface SpringDataFeedbackRepository :
    ReactiveCrudRepository<FeedbackEntity, UUID> {

    fun findByChannelId(channelId: UUID): Mono<FeedbackEntity>
}
```

#### SpringDataGroupRepository

```kotlin
interface SpringDataGroupRepository :
    ReactiveCrudRepository<GroupEntity, UUID> {

    fun findByIdAndDeletedFalse(id: UUID): Mono<GroupEntity>

    fun findAllByDeletedFalseOrderByCreatedAt(): Flux<GroupEntity>
}
```

#### SpringDataNotificationRepository

```kotlin
interface SpringDataNotificationRepository :
    ReactiveCrudRepository<NotificationEntity, UUID> {

    fun findByIdAndRecipientIdAndDeletedFalse(
        id: UUID,
        recipientId: UUID,
    ): Mono<NotificationEntity>

    @Modifying
    @Query("UPDATE notifications SET read = true WHERE id = :id AND recipient_id = :recipientId AND deleted = false")
    fun markAsRead(id: UUID, recipientId: UUID): Mono<Long>

    @Modifying
    @Query("UPDATE notifications SET read = true WHERE recipient_id = :recipientId AND read = false AND deleted = false")
    fun markAllAsReadByRecipientId(recipientId: UUID): Mono<Long>
}
```

#### SpringDataRecordingRepository

```kotlin
interface SpringDataRecordingRepository :
    ReactiveCrudRepository<RecordingEntity, UUID> {

    fun findByIdAndDeletedFalse(id: UUID): Mono<RecordingEntity>

    fun findByChannelIdAndStatusAndDeletedFalse(
        channelId: UUID,
        status: RecordingStatus,
    ): Mono<RecordingEntity>

    fun findAllByChannelIdAndDeletedFalseOrderByCreatedAt(channelId: UUID): Flux<RecordingEntity>
}
```

#### SpringDataScreenCaptureRepository

```kotlin
interface SpringDataScreenCaptureRepository :
    ReactiveCrudRepository<ScreenCaptureEntity, UUID> {

    fun findByIdAndDeletedFalse(id: UUID): Mono<ScreenCaptureEntity>

    @Modifying
    @Query("UPDATE screen_captures SET deleted = true WHERE id = :id")
    fun softDelete(id: UUID): Mono<Long>
}
```

#### SpringDataSharedFileRepository

```kotlin
interface SpringDataSharedFileRepository :
    ReactiveCrudRepository<SharedFileEntity, UUID> {

    fun findByIdAndDeletedFalse(id: UUID): Mono<SharedFileEntity>

    @Modifying
    @Query("UPDATE shared_files SET deleted = true WHERE id = :id")
    fun softDelete(id: UUID): Mono<Long>
}
```

### 5.2 api module -- Meta DB (`springdata/meta/`)

#### SpringDataTenantRepository

```kotlin
interface SpringDataTenantRepository :
    ReactiveCrudRepository<TenantEntity, UUID> {

    fun findByIdAndDeletedFalse(id: UUID): Mono<TenantEntity>

    fun findBySlugAndDeletedFalse(slug: String): Mono<TenantEntity>

    fun findAllByDeletedFalseOrderByCreatedAt(): Flux<TenantEntity>

    fun findAllByStatusAndDeletedFalseOrderByCreatedAt(status: TenantStatus): Flux<TenantEntity>
}
```

### 5.3 api-admin module -- Tenant DB (`springdata/tenant/`)

#### AdminSpringDataAgentRepository

```kotlin
interface AdminSpringDataAgentRepository :
    ReactiveCrudRepository<AgentEntity, UUID> {

    fun findByIdAndDeletedFalse(id: UUID): Mono<AgentEntity>

    fun findByUsernameAndDeletedFalse(username: String): Mono<AgentEntity>

    fun findAllByDeletedFalseOrderByCreatedAtDesc(): Flux<AgentEntity>

    fun findAllByGroupIdAndDeletedFalse(groupId: UUID): Flux<AgentEntity>

    fun countByDeletedFalse(): Mono<Long>

    fun countByGroupIdAndDeletedFalse(groupId: UUID): Mono<Long>
}
```

Pagination and search methods stay as `DatabaseClient` raw SQL (dynamic WHERE).

#### AdminSpringDataChannelRepository

```kotlin
interface AdminSpringDataChannelRepository :
    ReactiveCrudRepository<ChannelEntity, UUID> {

    fun findAllByStatusInOrderByCreatedAtDesc(
        status: Collection<ChannelStatus>,
    ): Flux<ChannelEntity>
}
```

#### AdminSpringDataCompanyRepository

```kotlin
interface AdminSpringDataCompanyRepository :
    ReactiveCrudRepository<CompanyEntity, UUID> {

    fun findFirstBy(): Mono<CompanyEntity>
}
```

#### AdminSpringDataGroupRepository

```kotlin
interface AdminSpringDataGroupRepository :
    ReactiveCrudRepository<GroupEntity, UUID> {

    fun findByIdAndDeletedFalse(id: UUID): Mono<GroupEntity>

    fun findAllByDeletedFalse(): Flux<GroupEntity>

    fun findByNameAndDeletedFalse(name: String): Mono<GroupEntity>

    fun countByDeletedFalse(): Mono<Long>
}
```

Pagination and search methods stay as `DatabaseClient` raw SQL (dynamic WHERE).

### 5.4 api-admin module -- Meta DB (`springdata/meta/`)

#### AdminSpringDataSuperAdminRepository

```kotlin
interface AdminSpringDataSuperAdminRepository :
    ReactiveCrudRepository<SuperAdminEntity, UUID> {

    fun findByUsernameAndDeletedFalse(username: String): Mono<SuperAdminEntity>
}
```

#### AdminSpringDataTenantRepository

```kotlin
interface AdminSpringDataTenantRepository :
    ReactiveCrudRepository<TenantEntity, UUID> {

    fun findByIdAndDeletedFalse(id: UUID): Mono<TenantEntity>

    fun findBySlugAndDeletedFalse(slug: String): Mono<TenantEntity>

    fun findByDbHostAndDbPortAndDeletedFalse(dbHost: String, dbPort: Int): Mono<TenantEntity>

    fun findAllByStatusAndDeletedFalse(status: TenantStatus): Flux<TenantEntity>

    fun countByDeletedFalse(): Mono<Long>

    fun countByStatusAndDeletedFalse(status: TenantStatus): Mono<Long>
}
```

Pagination and search methods stay as `DatabaseClient` raw SQL (dynamic WHERE).

---

## 6. Method-by-Method Mapping

### Legend
- **D** = Derived query (Spring Data interface method)
- **@Q** = `@Query` annotation on Spring Data interface
- **@M** = `@Modifying @Query` annotation
- **RAW** = `DatabaseClient` raw SQL (stays in adapter class)

### 6.1 api module

| Port Method | Strategy | Spring Data Method / Raw SQL |
|---|---|---|
| **AgentRepository** | | |
| `findByUsernameAndNotDeleted(username)` | **D** | `findByUsernameAndDeletedFalse(username)` |
| `findByIdAndNotDeleted(id)` | **D** | `findByIdAndDeletedFalse(id)` |
| `save(agent)` | **RAW** | `ON CONFLICT (id) DO UPDATE SET ...` |
| `findAllByGroupIdAndNotDeleted(groupId)` | **D** | `findAllByGroupIdAndDeletedFalse(groupId)` |
| `findAllByNotDeleted()` | **D** | `findAllByDeletedFalse()` |
| **ChannelRepository** | | |
| `save(channel)` | **RAW** | `ON CONFLICT (id) DO UPDATE SET ...` |
| `findByIdAndNotDeleted(id)` | **D** | `findByIdAndDeletedFalse(id)` |
| `findAllByAgentIdAndNotDeleted(agentId)` | **D** | `findAllByAgentIdAndDeletedFalseOrderByCreatedAt(agentId)` |
| `findAllByStatusAndNotDeleted(status)` | **D** | `findAllByStatusAndDeletedFalseOrderByCreatedAt(status)` |
| `findAllByAgentIdAndStatusAndNotDeleted(agentId, status)` | **D** | `findAllByAgentIdAndStatusAndDeletedFalseOrderByCreatedAt(agentId, status)` |
| `findByLivekitRoomNameAndNotDeleted(roomName)` | **D** | `findByLivekitRoomNameAndDeletedFalse(roomName)` |
| **ChatMessageRepository** | | |
| `save(message)` | **RAW** | Plain INSERT (no ON CONFLICT, but uses raw SQL for INSERT consistency) |
| `findAllByChannelId(channelId)` | **D** | `findAllByChannelIdOrderByCreatedAt(channelId)` |
| `findByChannelIdBefore(channelId, before, limit)` | **D** | `findAllByChannelIdAndCreatedAtLessThanOrderByCreatedAtDesc(channelId, before, Pageable.ofSize(limit))` |
| **CoBrowsingSessionRepository** | | |
| `save(session)` | **RAW** | `ON CONFLICT (id) DO UPDATE SET ...` |
| `findByIdAndNotDeleted(id)` | **D** | `findByIdAndDeletedFalse(id)` |
| `findActiveByChannelId(channelId)` | **D** | `findByChannelIdAndDeletedFalseAndStatusIn(channelId, listOf(REQUESTED, ACTIVE))` |
| **CompanyRepository** | | |
| `save(company)` | **RAW** | `ON CONFLICT (id) DO UPDATE SET ...` |
| `findFirst()` | **D** | `findFirstBy()` |
| **CounselNoteRepository** | | |
| `save(note)` | **RAW** | `ON CONFLICT (id) DO UPDATE SET ...` |
| `findByIdAndNotDeleted(id)` | **D** | `findByIdAndDeletedFalse(id)` |
| `findAllByChannelIdAndNotDeleted(channelId)` | **D** | `findAllByChannelIdAndDeletedFalseOrderByCreatedAt(channelId)` |
| **EndpointRepository** | | |
| `save(endpoint)` | **RAW** | `ON CONFLICT (id) DO UPDATE SET ...` |
| `findAllByChannelId(channelId)` | **D** | `findAllByChannelIdOrderByJoinedAt(channelId)` |
| **FeedbackRepository** | | |
| `save(feedback)` | **RAW** | `ON CONFLICT (channel_id) DO NOTHING` |
| `findByChannelId(channelId)` | **D** | `findByChannelId(channelId)` |
| **GroupRepository** | | |
| `save(group)` | **RAW** | `ON CONFLICT (id) DO UPDATE SET ...` |
| `findByIdAndNotDeleted(id)` | **D** | `findByIdAndDeletedFalse(id)` |
| `findAllByNotDeleted()` | **D** | `findAllByDeletedFalseOrderByCreatedAt()` |
| **NotificationRepository** | | |
| `save(notification)` | **RAW** | `INSERT ... RETURNING *` |
| `findByIdAndRecipientId(id, recipientId)` | **D** | `findByIdAndRecipientIdAndDeletedFalse(id, recipientId)` |
| `markAsRead(id, recipientId)` | **@M** | `UPDATE ... SET read = true WHERE id = :id AND recipient_id = :recipientId ...` |
| `markAllAsReadByRecipientId(recipientId)` | **@M** | `UPDATE ... SET read = true WHERE recipient_id = :recipientId AND read = false ...` |
| **RecordingRepository** | | |
| `save(recording)` | **RAW** | `ON CONFLICT (id) DO UPDATE SET ...` |
| `findByIdAndNotDeleted(id)` | **D** | `findByIdAndDeletedFalse(id)` |
| `findActiveByChannelId(channelId)` | **D** | `findByChannelIdAndStatusAndDeletedFalse(channelId, RECORDING)` |
| `findAllByChannelIdAndNotDeleted(channelId)` | **D** | `findAllByChannelIdAndDeletedFalseOrderByCreatedAt(channelId)` |
| **ScreenCaptureRepository** | | |
| `save(capture)` | **RAW** | Plain INSERT |
| `findByIdAndNotDeleted(id)` | **D** | `findByIdAndDeletedFalse(id)` |
| `softDelete(id)` | **@M** | `UPDATE screen_captures SET deleted = true WHERE id = :id` |
| **SharedFileRepository** | | |
| `save(file)` | **RAW** | Plain INSERT |
| `findByIdAndNotDeleted(id)` | **D** | `findByIdAndDeletedFalse(id)` |
| `softDelete(id)` | **@M** | `UPDATE shared_files SET deleted = true WHERE id = :id` |
| **TenantRepository** (meta DB) | | |
| `save(tenant)` | **RAW** | `ON CONFLICT (id) DO UPDATE SET ...` |
| `findById(id)` | **D** | `findByIdAndDeletedFalse(id)` |
| `findBySlug(slug)` | **D** | `findBySlugAndDeletedFalse(slug)` |
| `findAllByDeletedFalse()` | **D** | `findAllByDeletedFalseOrderByCreatedAt()` |
| `findAllByStatusAndDeletedFalse(status)` | **D** | `findAllByStatusAndDeletedFalseOrderByCreatedAt(TenantStatus.valueOf(status))` |

### 6.2 api-admin module

| Port Method | Strategy | Spring Data Method / Raw SQL |
|---|---|---|
| **AdminAgentRepository** | | |
| `save(agent)` | **RAW** | `ON CONFLICT (id) DO UPDATE SET ...` |
| `findByIdAndNotDeleted(id)` | **D** | `findByIdAndDeletedFalse(id)` |
| `findByUsernameAndNotDeleted(username)` | **D** | `findByUsernameAndDeletedFalse(username)` |
| `findAllByNotDeleted()` | **D** | `findAllByDeletedFalseOrderByCreatedAtDesc()` |
| `findAllByNotDeleted(page, size)` | **RAW** | `SELECT ... LIMIT :limit OFFSET :offset` |
| `countAllByNotDeleted()` | **D** | `countByDeletedFalse()` |
| `findAllByGroupIdAndNotDeleted(groupId)` | **D** | `findAllByGroupIdAndDeletedFalse(groupId)` |
| `findAllByGroupIdAndNotDeleted(groupId, page, size)` | **RAW** | `SELECT ... WHERE group_id = :groupId ... LIMIT :limit OFFSET :offset` |
| `countAllByGroupIdAndNotDeleted(groupId)` | **D** | `countByGroupIdAndDeletedFalse(groupId)` |
| `countByGroupIdAndNotDeleted(groupId)` | **D** | `countByGroupIdAndDeletedFalse(groupId)` (same as above) |
| `searchByNotDeleted(search, role, active, agentStatus, page, size)` | **RAW** | Dynamic WHERE with `DatabaseClient` |
| `countSearchByNotDeleted(search, role, active, agentStatus)` | **RAW** | Dynamic WHERE with `DatabaseClient` |
| **AdminChannelRepository** | | |
| `findAllActiveChannels()` | **D** | `findAllByStatusInOrderByCreatedAtDesc(listOf(WAITING, IN_PROGRESS))` |
| **AdminCompanyRepository** | | |
| `save(company)` | **RAW** | `ON CONFLICT (id) DO UPDATE SET ...` |
| `findFirst()` | **D** | `findFirstBy()` |
| **AdminGroupRepository** | | |
| `save(group)` | **RAW** | `ON CONFLICT (id) DO UPDATE SET ...` |
| `findByIdAndNotDeleted(id)` | **D** | `findByIdAndDeletedFalse(id)` |
| `findAllByNotDeleted()` | **D** | `findAllByDeletedFalse()` |
| `findAllByNotDeleted(page, size)` | **RAW** | `SELECT ... LIMIT :limit OFFSET :offset` |
| `countAllByNotDeleted()` | **D** | `countByDeletedFalse()` |
| `findByNameAndNotDeleted(name)` | **D** | `findByNameAndDeletedFalse(name)` |
| `searchByNotDeleted(search, status, page, size)` | **RAW** | Dynamic WHERE with `DatabaseClient` |
| `countSearchByNotDeleted(search, status)` | **RAW** | Dynamic WHERE with `DatabaseClient` |
| **AdminSuperAdminRepository** (meta DB) | | |
| `findByUsernameAndNotDeleted(username)` | **D** | `findByUsernameAndDeletedFalse(username)` |
| **AdminTenantRepository** (meta DB) | | |
| `save(tenant)` | **RAW** | `ON CONFLICT (id) DO UPDATE SET ...` |
| `findById(id)` | **D** | `findByIdAndDeletedFalse(id)` |
| `findBySlug(slug)` | **D** | `findBySlugAndDeletedFalse(slug)` |
| `findByDbHostAndDbPort(dbHost, dbPort)` | **D** | `findByDbHostAndDbPortAndDeletedFalse(dbHost, dbPort)` |
| `findAllByDeletedFalse(page, size)` | **RAW** | `SELECT ... LIMIT :limit OFFSET :offset` |
| `countAllByDeletedFalse()` | **D** | `countByDeletedFalse()` |
| `findAllByStatusAndDeletedFalse(status)` | **D** | `findAllByStatusAndDeletedFalse(TenantStatus.valueOf(status))` |
| `findAllByStatusAndDeletedFalse(status, page, size)` | **RAW** | `SELECT ... WHERE status = :status ... LIMIT :limit OFFSET :offset` |
| `countAllByStatusAndDeletedFalse(status)` | **D** | `countByStatusAndDeletedFalse(TenantStatus.valueOf(status))` |
| `searchByDeletedFalse(search, status, page, size)` | **RAW** | Dynamic WHERE with `DatabaseClient` |
| `countSearchByDeletedFalse(search, status)` | **RAW** | Dynamic WHERE with `DatabaseClient` |

---

## 7. Adapter Class Refactoring Pattern

### Before (current)

```kotlin
@Repository
@Profile("!test")
class AgentR2dbcRepository(
    private val template: R2dbcEntityTemplate,
) : AgentRepository {
    private val client = template.databaseClient

    override fun findByUsernameAndNotDeleted(username: String): Mono<Agent> =
        template
            .select(AgentEntity::class.java)
            .matching(query(where("username").`is`(username).and("deleted").`is`(false)))
            .one()
            .map { it.toDomain() }

    override fun save(agent: Agent): Mono<Agent> {
        // 30+ lines of raw SQL with nullable bindings
    }
}
```

### After (refactored)

```kotlin
@Repository
@Profile("!test")
class AgentR2dbcRepository(
    private val springData: SpringDataAgentRepository,
    private val template: R2dbcEntityTemplate,
) : AgentRepository {
    private val client = template.databaseClient

    override fun findByUsernameAndNotDeleted(username: String): Mono<Agent> =
        springData.findByUsernameAndDeletedFalse(username).map { it.toDomain() }

    override fun findByIdAndNotDeleted(id: UUID): Mono<Agent> =
        springData.findByIdAndDeletedFalse(id).map { it.toDomain() }

    override fun findAllByGroupIdAndNotDeleted(groupId: UUID): Flux<Agent> =
        springData.findAllByGroupIdAndDeletedFalse(groupId).map { it.toDomain() }

    override fun findAllByNotDeleted(): Flux<Agent> =
        springData.findAllByDeletedFalse().map { it.toDomain() }

    override fun save(agent: Agent): Mono<Agent> {
        // Stays as raw SQL -- ON CONFLICT upsert
        val spec = client.sql("""
            INSERT INTO agents (...) VALUES (...)
            ON CONFLICT (id) DO UPDATE SET ...
        """.trimIndent())
        // ... same as current ...
    }
}
```

### Key changes per adapter:
1. Add `SpringData*Repository` as constructor parameter
2. Replace all `template.select(...)` calls with `springData.findBy*()` calls
3. Keep `save()` and any `@Modifying` operations that need raw SQL
4. For `@Modifying` methods on the Spring Data interface (markAsRead, softDelete), delegate directly

---

## 8. Handling @Modifying @Query Results

Spring Data R2DBC `@Modifying @Query` returns `Mono<Long>` (rows affected). The port interfaces expect different return types:

| Port Method | Port Return | @Modifying Return | Adapter Conversion |
|---|---|---|---|
| `NotificationRepository.markAsRead(id, recipientId)` | `Mono<Boolean>` | `Mono<Long>` | `.map { it > 0 }` |
| `NotificationRepository.markAllAsReadByRecipientId(recipientId)` | `Mono<Long>` | `Mono<Long>` | Direct delegation |
| `ScreenCaptureRepository.softDelete(id)` | `Mono<Void>` | `Mono<Long>` | `.then()` |
| `SharedFileRepository.softDelete(id)` | `Mono<Void>` | `Mono<Long>` | `.then()` |

---

## 9. Pagination for Admin Search Methods

The paginated methods in `api-admin` (e.g., `findAllByNotDeleted(page, size)`) and search methods (e.g., `searchByNotDeleted(...)`) remain as `DatabaseClient` raw SQL because:

1. **Dynamic WHERE clauses** -- search methods build SQL conditionally based on which filter params are non-null. Spring Data cannot express optional parameters in derived queries.
2. **LIMIT/OFFSET pagination** -- While Spring Data supports `Pageable`, combining it with the tenant-routed `DatabaseClient` and dynamic filters is simpler with raw SQL.

These methods stay unchanged in the adapter classes.

---

## 10. R2dbcConfig Changes Summary

### api module (`R2dbcConfig.kt`)

1. **Remove** `@EnableR2dbcRepositories` from `ApiApplication` if present (Spring Boot auto-configuration)
2. **Add** two new `@Configuration` classes (or add to existing `R2dbcConfig`):

```kotlin
@Configuration
@Profile("!test")
@EnableR2dbcRepositories(
    basePackages = ["com.counseling.api.adapter.outbound.persistence.springdata.tenant"],
    entityOperationsRef = "tenantR2dbcEntityTemplate"
)
class TenantR2dbcRepositoryConfig

@Configuration
@Profile("!test")
@EnableR2dbcRepositories(
    basePackages = ["com.counseling.api.adapter.outbound.persistence.springdata.meta"],
    entityOperationsRef = "metaR2dbcEntityTemplate"
)
class MetaR2dbcRepositoryConfig
```

3. **Keep** all existing beans (`metaConnectionFactory`, `tenantConnectionFactory`, `tenantR2dbcEntityTemplate`, `metaR2dbcEntityTemplate`, `DatabaseClient` beans) -- Spring Data interfaces need the `R2dbcEntityTemplate` beans for entity operations.

### api-admin module (`R2dbcConfig.kt`)

Same pattern as above with `com.counseling.admin.adapter.outbound.persistence.springdata.tenant` and `com.counseling.admin.adapter.outbound.persistence.springdata.meta`.

---

## 11. Files to Create

### api module -- NEW files

| File | Description |
|---|---|
| `.../springdata/tenant/SpringDataAgentRepository.kt` | `ReactiveCrudRepository<AgentEntity, UUID>` |
| `.../springdata/tenant/SpringDataChannelRepository.kt` | `ReactiveCrudRepository<ChannelEntity, UUID>` |
| `.../springdata/tenant/SpringDataChatMessageRepository.kt` | `ReactiveCrudRepository<ChatMessageEntity, UUID>` |
| `.../springdata/tenant/SpringDataCoBrowsingSessionRepository.kt` | `ReactiveCrudRepository<CoBrowsingSessionEntity, UUID>` |
| `.../springdata/tenant/SpringDataCompanyRepository.kt` | `ReactiveCrudRepository<CompanyEntity, UUID>` |
| `.../springdata/tenant/SpringDataCounselNoteRepository.kt` | `ReactiveCrudRepository<CounselNoteEntity, UUID>` |
| `.../springdata/tenant/SpringDataEndpointRepository.kt` | `ReactiveCrudRepository<EndpointEntity, UUID>` |
| `.../springdata/tenant/SpringDataFeedbackRepository.kt` | `ReactiveCrudRepository<FeedbackEntity, UUID>` |
| `.../springdata/tenant/SpringDataGroupRepository.kt` | `ReactiveCrudRepository<GroupEntity, UUID>` |
| `.../springdata/tenant/SpringDataNotificationRepository.kt` | `ReactiveCrudRepository<NotificationEntity, UUID>` with `@Modifying @Query` |
| `.../springdata/tenant/SpringDataRecordingRepository.kt` | `ReactiveCrudRepository<RecordingEntity, UUID>` |
| `.../springdata/tenant/SpringDataScreenCaptureRepository.kt` | `ReactiveCrudRepository<ScreenCaptureEntity, UUID>` with `@Modifying @Query` |
| `.../springdata/tenant/SpringDataSharedFileRepository.kt` | `ReactiveCrudRepository<SharedFileEntity, UUID>` with `@Modifying @Query` |
| `.../springdata/meta/SpringDataTenantRepository.kt` | `ReactiveCrudRepository<TenantEntity, UUID>` |
| `.../config/TenantR2dbcRepositoryConfig.kt` | `@EnableR2dbcRepositories` for tenant package |
| `.../config/MetaR2dbcRepositoryConfig.kt` | `@EnableR2dbcRepositories` for meta package |

### api-admin module -- NEW files

| File | Description |
|---|---|
| `.../springdata/tenant/AdminSpringDataAgentRepository.kt` | `ReactiveCrudRepository<AgentEntity, UUID>` |
| `.../springdata/tenant/AdminSpringDataChannelRepository.kt` | `ReactiveCrudRepository<ChannelEntity, UUID>` |
| `.../springdata/tenant/AdminSpringDataCompanyRepository.kt` | `ReactiveCrudRepository<CompanyEntity, UUID>` |
| `.../springdata/tenant/AdminSpringDataGroupRepository.kt` | `ReactiveCrudRepository<GroupEntity, UUID>` |
| `.../springdata/meta/AdminSpringDataSuperAdminRepository.kt` | `ReactiveCrudRepository<SuperAdminEntity, UUID>` |
| `.../springdata/meta/AdminSpringDataTenantRepository.kt` | `ReactiveCrudRepository<TenantEntity, UUID>` |
| `.../config/TenantR2dbcRepositoryConfig.kt` | `@EnableR2dbcRepositories` for tenant package |
| `.../config/MetaR2dbcRepositoryConfig.kt` | `@EnableR2dbcRepositories` for meta package |

## 12. Files to Modify

### api module

| File | Changes |
|---|---|
| `AgentR2dbcRepository.kt` | Add `SpringDataAgentRepository` param; replace template queries with springData calls |
| `ChannelR2dbcRepository.kt` | Add `SpringDataChannelRepository` param; replace template queries |
| `ChatMessageR2dbcRepository.kt` | Add `SpringDataChatMessageRepository` param; replace template queries |
| `CoBrowsingSessionR2dbcRepository.kt` | Add `SpringDataCoBrowsingSessionRepository` param; replace template queries |
| `CompanyR2dbcRepository.kt` | Add `SpringDataCompanyRepository` param; replace template queries |
| `CounselNoteR2dbcRepository.kt` | Add `SpringDataCounselNoteRepository` param; replace template queries |
| `EndpointR2dbcRepository.kt` | Add `SpringDataEndpointRepository` param; replace template queries |
| `FeedbackR2dbcRepository.kt` | Add `SpringDataFeedbackRepository` param; replace template query |
| `GroupR2dbcRepository.kt` | Add `SpringDataGroupRepository` param; replace template queries |
| `NotificationR2dbcRepository.kt` | Add `SpringDataNotificationRepository` param; replace template queries + markAsRead/markAllAsRead |
| `RecordingR2dbcRepository.kt` | Add `SpringDataRecordingRepository` param; replace template queries |
| `ScreenCaptureR2dbcRepository.kt` | Add `SpringDataScreenCaptureRepository` param; replace template queries + softDelete |
| `SharedFileR2dbcRepository.kt` | Add `SpringDataSharedFileRepository` param; replace template queries + softDelete |
| `TenantR2dbcRepository.kt` | Add `SpringDataTenantRepository` param; replace template queries |
| `R2dbcConfig.kt` | No changes to existing beans; new config classes added separately |

### api-admin module

| File | Changes |
|---|---|
| `AdminAgentR2dbcRepository.kt` | Add `AdminSpringDataAgentRepository` param; replace template queries |
| `AdminChannelR2dbcRepository.kt` | Add `AdminSpringDataChannelRepository` param; replace template query |
| `AdminCompanyR2dbcRepository.kt` | Add `AdminSpringDataCompanyRepository` param; replace template query |
| `AdminGroupR2dbcRepository.kt` | Add `AdminSpringDataGroupRepository` param; replace template queries |
| `AdminSuperAdminR2dbcRepository.kt` | Add `AdminSpringDataSuperAdminRepository` param; replace template query |
| `AdminTenantR2dbcRepository.kt` | Add `AdminSpringDataTenantRepository` param; replace template queries |
| `R2dbcConfig.kt` | No changes to existing beans; new config classes added separately |

---

## 13. Migration Checklist

1. Create `springdata/tenant/` and `springdata/meta/` packages in both modules
2. Create all Spring Data interface files
3. Create `@EnableR2dbcRepositories` configuration classes
4. Refactor each adapter class to inject Spring Data interface and delegate query methods
5. Remove `R2dbcEntityTemplate` usage from adapter classes where no longer needed (keep `template` only if `databaseClient` is still used for save/upsert)
6. Simplify adapter constructors: if an adapter only needs `DatabaseClient` (for save) and Spring Data (for queries), inject `DatabaseClient` directly via `@Qualifier` instead of `R2dbcEntityTemplate`
7. Run existing tests -- all port interface contracts remain unchanged
8. Verify multi-tenant routing works by testing against real tenant databases

---

## 14. Risk Assessment

| Risk | Mitigation |
|---|---|
| Spring Data scans wrong package and wires wrong ConnectionFactory | Sub-package separation (`tenant/` vs `meta/`) makes this deterministic |
| `findFirstBy()` returns nothing when table is empty | Same behavior as current `template.select().limit(1)` -- returns empty Mono |
| Enum conversion differs from raw SQL | Spring Data R2DBC uses same default `EnumWriteSupport` -- tested with existing entities |
| `@Modifying @Query` returns `Mono<Long>` vs port expectations | Adapter conversion layer handles (`.map { it > 0 }`, `.then()`) |
| Circular bean dependency between template and Spring Data repos | Not an issue -- Spring Data repos depend on `R2dbcEntityOperations` (the template), not vice versa |
| `@Profile("!test")` on adapter classes conflicts with Spring Data auto-wiring | Spring Data interfaces are separate beans; adapter classes with `@Profile("!test")` are the port implementations. Test mocks go against port interfaces, not Spring Data interfaces. |
