# Persistence Layer Simplification

## Problem

Current read path has 5 layers:

```
Service -> Port Interface -> Adapter Class -> Spring Data Interface -> Entity Class -> Domain Model
```

The Entity classes are pure boilerplate -- they duplicate every field from the Domain model, adding only `@Table`/`@Column` annotations and a `toDomain()` method that copies field-by-field.

## Target Architecture

```
Service -> Port Interface (= Spring Data + Custom) -> Domain Model (annotated)
```

**3 layers.** Port interface IS the Spring Data interface, extended with a custom implementation for upsert/dynamic queries.

### What gets eliminated

- **Entity classes** (14 in api, 6 in api-admin) -- annotations move to domain models
- **Adapter classes** that only delegate Spring Data calls with `.map { it.toDomain() }` -- the Spring Data interface itself becomes the port
- **`toDomain()` / `fromDomain()` mapping** -- gone entirely

### What stays

- Domain models (now annotated with `@Table`/`@Id`/`@Column`)
- Custom implementation classes for upsert (`save`) and dynamic search -- but scoped to only those methods
- Spring Data interfaces (merged with port interfaces)

---

## Decisions

### Q1: Merge Port Interface + Spring Data Interface

**Decision: Option C -- Port interface extends ReactiveCrudRepository with Spring Data naming.**

The port interface directly extends `ReactiveCrudRepository<DomainModel, UUID>` and uses Spring Data query derivation naming. Services update their calls to match.

Rationale:
- Spring Data R2DBC already uses the `@Table`-annotated class as both the entity and the return type. There is no JPA-style proxy; the domain model IS the row mapping target.
- Spring Data query derivation naming (`findByIdAndDeletedFalse`) is declarative and self-documenting.
- The "port as abstraction" benefit is minimal here -- these are simple CRUD queries, not complex business logic. The port interface was only hiding Spring Data naming conventions behind aliases (`findByIdAndNotDeleted` -> `findByIdAndDeletedFalse`), which adds a layer with zero value.

Impact on services:
- `findByIdAndNotDeleted(id)` -> `findByIdAndDeletedFalse(id)`
- `findAllByNotDeleted()` -> `findAllByDeletedFalse()`
- `findByUsernameAndNotDeleted(username)` -> `findByUsernameAndDeletedFalse(username)`

This is a mechanical rename. The method signatures are equally readable and now match what the DB actually does.

### Q2: Upsert / Save Methods

**Decision: Spring Data custom implementation pattern.**

```
AgentRepository (interface)
  extends ReactiveCrudRepository<Agent, UUID>
  extends AgentRepositoryCustom

AgentRepositoryCustom (interface)
  fun upsert(agent: Agent): Mono<Agent>

AgentRepositoryCustomImpl (class, @Component)
  implements AgentRepositoryCustom
  uses DatabaseClient for INSERT...ON CONFLICT
```

Method is named `upsert()` (not `save()`) to avoid collision with `ReactiveCrudRepository.save()`.

Spring Data auto-discovers `*Impl` suffix classes. The composite interface (`AgentRepository`) inherits both Spring Data derived queries AND the custom `upsert()` method. Services call `agentRepository.upsert(agent)` for create/update, and `agentRepository.findByIdAndDeletedFalse(id)` for reads.

The custom impl class lives in the same package as the repository interface so Spring Data can find it.

### Q3: Admin vs API Modules Sharing Domain Models

**Decision: Keep separate domain models. Unify the DB table mapping via a single superset `@Table` class in a shared module is NOT worth the complexity.**

Rationale:
- Admin `Agent` has `active` (no `email`), API `Agent` has `email` (no `active`). These reflect genuinely different bounded contexts.
- The DB table has ALL columns. Each module's domain model maps only the columns it cares about. Spring Data R2DBC ignores unmapped columns on read and uses defaults on write.
- A shared model would force both modules to carry fields they never use and add coupling between independently deployable services.
- The two modules already have separate packages (`com.counseling.api.domain` vs `com.counseling.admin.domain`), separate Gradle projects, and separate deployment units. This is correct.

Practical note: Spring Data R2DBC's `DefaultReactiveDataAccessStrategy` maps row columns to constructor parameters by name. If API's `Agent` lacks `active`, the column is simply ignored during read. On write (upsert), the INSERT statement explicitly names its columns, so missing fields are fine -- they get the DB default.

### Q4: Dynamic Search Queries

**Decision: Custom implementation class, same pattern as Q2.**

```
AdminAgentRepository (interface)
  extends ReactiveCrudRepository<Agent, UUID>
  extends AdminAgentRepositoryCustom

AdminAgentRepositoryCustom (interface)
  fun upsert(agent: Agent): Mono<Agent>
  fun searchByNotDeleted(...): Flux<Agent>
  fun countSearchByNotDeleted(...): Mono<Long>

AdminAgentRepositoryCustomImpl (class)
  uses DatabaseClient to build dynamic WHERE clauses
  maps rows directly to domain model (no entity intermediary)
```

The `mapToAgent(row: Readable)` helper constructs the domain `Agent` directly from the R2DBC `Readable`. This eliminates the entity class entirely even for dynamic queries.

### Q5: @Modifying Queries (softDelete, markAsRead)

**Decision: `@Modifying @Query` on the Spring Data interface for simple updates. Custom impl for complex ones.**

Simple cases (NotificationRepository):
```kotlin
interface NotificationRepository : ReactiveCrudRepository<Notification, UUID> {
    @Modifying
    @Query("UPDATE notifications SET read = true WHERE id = :id AND recipient_id = :recipientId")
    fun markAsRead(id: UUID, recipientId: UUID): Mono<Boolean>

    @Modifying
    @Query("UPDATE notifications SET read = true WHERE recipient_id = :recipientId")
    fun markAllAsReadByRecipientId(recipientId: UUID): Mono<Long>
}
```

No custom impl needed for these -- `@Modifying @Query` handles them directly on the interface.

---

## Domain Model Changes

Add `@Table`, `@Id`, and `@Column` annotations to domain data classes. Spring Data R2DBC uses `NamingStrategy` (default: `snake_case`) to map `camelCase` fields to `snake_case` columns. However, the default strategy maps `createdAt` -> `created_at` only if using `R2dbcEntityTemplate`. For `ReactiveCrudRepository` query derivation, the column mapping relies on `@Column` or the configured `NamingStrategy`.

Since the project already configures `R2dbcEntityTemplate` with `PostgresDialect` (which includes snake_case naming), most fields auto-map. Explicit `@Column` is only needed when the property name does not follow the standard camelCase-to-snake_case conversion, or when we want to be defensive.

**Recommended approach: keep `@Column` annotations for clarity and safety.** The cost is minimal (one annotation per field) and it prevents subtle bugs if naming strategy changes.

### Example: Agent (api module)

```kotlin
// BEFORE: domain/Agent.kt (no annotations)
data class Agent(
    val id: UUID,
    val username: String,
    val passwordHash: String,
    ...
)

// AFTER: domain/Agent.kt (annotated)
@Table("agents")
data class Agent(
    @Id
    val id: UUID,
    val username: String,
    @Column("password_hash")
    val passwordHash: String,
    val name: String,
    val role: AgentRole,
    @Column("created_at")
    val createdAt: Instant,
    @Column("updated_at")
    val updatedAt: Instant,
    val deleted: Boolean = false,
    @Column("group_id")
    val groupId: UUID? = null,
    @Column("agent_status")
    val agentStatus: AgentStatus = AgentStatus.OFFLINE,
    val email: String? = null,
) {
    // Business methods unchanged
    fun changePassword(newHash: String): Agent = copy(passwordHash = newHash, updatedAt = Instant.now())
    fun changeName(newName: String): Agent = copy(name = newName, updatedAt = Instant.now())
    fun isActive(): Boolean = !deleted
    fun updateStatus(status: AgentStatus): Agent = copy(agentStatus = status, updatedAt = Instant.now())
    fun assignToGroup(groupId: UUID?): Agent = copy(groupId = groupId, updatedAt = Instant.now())
    fun isAvailable(): Boolean = !deleted && agentStatus == AgentStatus.ONLINE
}
```

The domain model remains a data class with business methods. The only addition is 4-5 annotations. This is a pragmatic trade-off: we sacrifice "pure domain model" purity to eliminate an entire layer of boilerplate.

---

## New Repository Structure

### Example: AgentRepository (api module)

```kotlin
// port/outbound/AgentRepository.kt
// This IS the Spring Data interface. No separate SpringDataAgentRepository.
interface AgentRepository :
    ReactiveCrudRepository<Agent, UUID>,
    AgentRepositoryCustom {

    fun findByUsernameAndDeletedFalse(username: String): Mono<Agent>
    fun findByIdAndDeletedFalse(id: UUID): Mono<Agent>
    fun findAllByGroupIdAndDeletedFalse(groupId: UUID): Flux<Agent>
    fun findAllByDeletedFalse(): Flux<Agent>
}
```

```kotlin
// port/outbound/AgentRepositoryCustom.kt
interface AgentRepositoryCustom {
    fun upsert(agent: Agent): Mono<Agent>
}
```

```kotlin
// adapter/outbound/persistence/AgentRepositoryCustomImpl.kt
@Component
class AgentRepositoryCustomImpl(
    private val databaseClient: DatabaseClient,
) : AgentRepositoryCustom {

    override fun upsert(agent: Agent): Mono<Agent> {
        // Same INSERT...ON CONFLICT logic, binding directly from domain model
        return databaseClient.sql("""
            INSERT INTO agents (id, username, password_hash, ...)
            VALUES (:id, :username, :passwordHash, ...)
            ON CONFLICT (id) DO UPDATE SET ...
        """.trimIndent())
            .bind("id", agent.id)
            .bind("username", agent.username)
            // ... same bindings as today, but no entity conversion
            .then()
            .thenReturn(agent)
    }
}
```

### Package layout change

```
BEFORE:
  port/outbound/AgentRepository.kt                              (port interface)
  adapter/outbound/persistence/springdata/tenant/SpringDataAgentRepository.kt  (Spring Data)
  adapter/outbound/persistence/entity/AgentEntity.kt             (entity)
  adapter/outbound/persistence/AgentR2dbcRepository.kt           (adapter)

AFTER:
  port/outbound/AgentRepository.kt                              (Spring Data + port, merged)
  port/outbound/AgentRepositoryCustom.kt                         (custom method interface)
  adapter/outbound/persistence/AgentRepositoryCustomImpl.kt      (custom impl only)
```

4 files -> 3 files, and the impl file is much smaller (only upsert logic, no delegation boilerplate).

---

## Multi-Tenant Configuration Impact

The `@EnableR2dbcRepositories` configs currently point to:
- `com.counseling.api.adapter.outbound.persistence.springdata.tenant` (tenant repos)
- `com.counseling.api.adapter.outbound.persistence.springdata.meta` (meta repos)

After merging, the port interfaces move to `port/outbound/`. We need two sub-packages:

```
port/outbound/tenant/AgentRepository.kt
port/outbound/tenant/ChannelRepository.kt
port/outbound/tenant/...
port/outbound/meta/TenantRepository.kt
```

Update configs:
```kotlin
@EnableR2dbcRepositories(
    basePackages = ["com.counseling.api.port.outbound.tenant"],
    entityOperationsRef = "tenantR2dbcEntityTemplate",
)

@EnableR2dbcRepositories(
    basePackages = ["com.counseling.api.port.outbound.meta"],
    entityOperationsRef = "metaR2dbcEntityTemplate",
)
```

This keeps the multi-tenant routing intact while eliminating the Spring Data sub-package under adapters.

---

## File-by-File Plan

### api module

#### DELETE (28 files)

Entity classes (14 files):
- `adapter/outbound/persistence/entity/AgentEntity.kt`
- `adapter/outbound/persistence/entity/ChannelEntity.kt`
- `adapter/outbound/persistence/entity/ChatMessageEntity.kt`
- `adapter/outbound/persistence/entity/CoBrowsingSessionEntity.kt`
- `adapter/outbound/persistence/entity/CompanyEntity.kt`
- `adapter/outbound/persistence/entity/CounselNoteEntity.kt`
- `adapter/outbound/persistence/entity/EndpointEntity.kt`
- `adapter/outbound/persistence/entity/FeedbackEntity.kt`
- `adapter/outbound/persistence/entity/GroupEntity.kt`
- `adapter/outbound/persistence/entity/NotificationEntity.kt`
- `adapter/outbound/persistence/entity/RecordingEntity.kt`
- `adapter/outbound/persistence/entity/ScreenCaptureEntity.kt`
- `adapter/outbound/persistence/entity/SharedFileEntity.kt`
- `adapter/outbound/persistence/entity/TenantEntity.kt`

Spring Data interfaces (14 files -- absorbed into port interfaces):
- `adapter/outbound/persistence/springdata/tenant/SpringDataAgentRepository.kt`
- `adapter/outbound/persistence/springdata/tenant/SpringDataChannelRepository.kt`
- `adapter/outbound/persistence/springdata/tenant/SpringDataChatMessageRepository.kt`
- `adapter/outbound/persistence/springdata/tenant/SpringDataCoBrowsingSessionRepository.kt`
- `adapter/outbound/persistence/springdata/tenant/SpringDataCompanyRepository.kt`
- `adapter/outbound/persistence/springdata/tenant/SpringDataCounselNoteRepository.kt`
- `adapter/outbound/persistence/springdata/tenant/SpringDataEndpointRepository.kt`
- `adapter/outbound/persistence/springdata/tenant/SpringDataFeedbackRepository.kt`
- `adapter/outbound/persistence/springdata/tenant/SpringDataGroupRepository.kt`
- `adapter/outbound/persistence/springdata/tenant/SpringDataNotificationRepository.kt`
- `adapter/outbound/persistence/springdata/tenant/SpringDataRecordingRepository.kt`
- `adapter/outbound/persistence/springdata/tenant/SpringDataScreenCaptureRepository.kt`
- `adapter/outbound/persistence/springdata/tenant/SpringDataSharedFileRepository.kt`
- `adapter/outbound/persistence/springdata/meta/SpringDataTenantRepository.kt`

#### MODIFY -> DELETE (adapter classes that become unnecessary)

These adapter classes currently do two things: (a) delegate to Spring Data + map entity->domain, and (b) upsert via DatabaseClient. After the merge, (a) is gone and (b) moves to `*RepositoryCustomImpl`. The adapter class itself is deleted.

- `adapter/outbound/persistence/AgentR2dbcRepository.kt` -> DELETE (upsert logic moves to `AgentRepositoryCustomImpl`)
- `adapter/outbound/persistence/CompanyR2dbcRepository.kt` -> DELETE
- `adapter/outbound/persistence/CounselNoteR2dbcRepository.kt` -> DELETE
- `adapter/outbound/persistence/FeedbackR2dbcRepository.kt` -> DELETE
- `adapter/outbound/persistence/RecordingR2dbcRepository.kt` -> DELETE
- `adapter/outbound/persistence/CoBrowsingSessionR2dbcRepository.kt` -> DELETE
- `adapter/outbound/persistence/TenantR2dbcRepository.kt` -> DELETE

#### CREATE (new files)

Custom interfaces + impls (one pair per entity that needs upsert or DatabaseClient logic):
- `port/outbound/tenant/AgentRepositoryCustom.kt` + `adapter/outbound/persistence/AgentRepositoryCustomImpl.kt`
- `port/outbound/tenant/ChannelRepositoryCustom.kt` + `adapter/outbound/persistence/ChannelRepositoryCustomImpl.kt`
- `port/outbound/tenant/CompanyRepositoryCustom.kt` + `adapter/outbound/persistence/CompanyRepositoryCustomImpl.kt`
- `port/outbound/tenant/CounselNoteRepositoryCustom.kt` + `adapter/outbound/persistence/CounselNoteRepositoryCustomImpl.kt`
- `port/outbound/tenant/FeedbackRepositoryCustom.kt` + `adapter/outbound/persistence/FeedbackRepositoryCustomImpl.kt`
- `port/outbound/tenant/RecordingRepositoryCustom.kt` + `adapter/outbound/persistence/RecordingRepositoryCustomImpl.kt`
- `port/outbound/tenant/CoBrowsingSessionRepositoryCustom.kt` + `adapter/outbound/persistence/CoBrowsingSessionRepositoryCustomImpl.kt`
- `port/outbound/meta/TenantRepositoryCustom.kt` + `adapter/outbound/persistence/TenantRepositoryCustomImpl.kt`

Note: entities that do NOT need upsert (e.g., simple find-only repos) need NO custom impl. Their port interface just extends `ReactiveCrudRepository` with query derivation methods.

#### MODIFY (existing files)

Domain models -- add `@Table`/`@Id`/`@Column` annotations (14 files):
- `domain/Agent.kt`
- `domain/Channel.kt`
- `domain/ChatMessage.kt`
- `domain/CoBrowsingSession.kt`
- `domain/Company.kt`
- `domain/CounselNote.kt`
- `domain/Endpoint.kt`
- `domain/Feedback.kt`
- `domain/Group.kt`
- `domain/Notification.kt`
- `domain/Recording.kt`
- `domain/ScreenCapture.kt`
- `domain/SharedFile.kt`
- `domain/Tenant.kt`

Port interfaces -- merge with Spring Data, move to tenant/meta sub-packages, rename methods:
- `port/outbound/AgentRepository.kt` -> `port/outbound/tenant/AgentRepository.kt` (extends ReactiveCrudRepository)
- `port/outbound/ChannelRepository.kt` -> `port/outbound/tenant/ChannelRepository.kt`
- `port/outbound/CompanyRepository.kt` -> `port/outbound/tenant/CompanyRepository.kt`
- `port/outbound/CounselNoteRepository.kt` -> `port/outbound/tenant/CounselNoteRepository.kt`
- `port/outbound/FeedbackRepository.kt` -> `port/outbound/tenant/FeedbackRepository.kt`
- `port/outbound/RecordingRepository.kt` -> `port/outbound/tenant/RecordingRepository.kt`
- `port/outbound/TenantRepository.kt` -> `port/outbound/meta/TenantRepository.kt`
- (and remaining repos similarly)

Config files:
- `config/TenantR2dbcRepositoryConfig.kt` -- update basePackages to `port.outbound.tenant`
- `config/MetaR2dbcRepositoryConfig.kt` -- update basePackages to `port.outbound.meta`

Service files -- update method call names:
- All services calling `findByIdAndNotDeleted()` -> `findByIdAndDeletedFalse()`
- All services calling `save()` -> `upsert()` (for R2DBC repos that use INSERT...ON CONFLICT)
- All services calling `findAllByNotDeleted()` -> `findAllByDeletedFalse()`
- Import paths change from `port.outbound.AgentRepository` to `port.outbound.tenant.AgentRepository`

### api-admin module

Same pattern. 6 entity classes deleted, 6 Spring Data interfaces absorbed, adapter classes replaced with custom impls.

---

## Impact Summary

| Metric | Before | After | Delta |
|--------|--------|-------|-------|
| Entity classes (api) | 14 | 0 | -14 |
| Entity classes (admin) | 6 | 0 | -6 |
| Spring Data interfaces (api) | 14 | 0 (merged into ports) | -14 |
| Spring Data interfaces (admin) | 6 | 0 (merged into ports) | -6 |
| Adapter classes (api) | ~7 | 0 | -7 |
| Adapter classes (admin) | ~6 | 0 | -6 |
| Custom impl classes (api) | 0 | ~8 | +8 |
| Custom impl classes (admin) | 0 | ~6 | +6 |
| Custom interface files (api) | 0 | ~8 | +8 |
| Custom interface files (admin) | 0 | ~6 | +6 |
| **Net files removed** | | | **~25** |

Lines of code reduction estimate: ~1500-2000 lines of pure boilerplate eliminated (entity classes + toDomain mapping + adapter delegation).

---

## Migration Strategy

### Phase 1: Annotate domain models (no behavior change)

Add `@Table`/`@Id`/`@Column` to all domain data classes. Nothing else changes. Entity classes still exist and still work. This phase is a no-op at runtime -- domain models gain annotations but are not used by Spring Data yet.

**Verify: full test suite passes.**

### Phase 2: Migrate one simple entity end-to-end (proof of concept)

Pick `Feedback` (simplest: 2 port methods, simple upsert, no dynamic queries).

1. Create `port/outbound/tenant/FeedbackRepository.kt` extending `ReactiveCrudRepository<Feedback, UUID>` + `FeedbackRepositoryCustom`
2. Create `port/outbound/tenant/FeedbackRepositoryCustom.kt` with `upsert()`
3. Create `adapter/outbound/persistence/FeedbackRepositoryCustomImpl.kt`
4. Update `FeedbackService` imports and method names
5. Delete `FeedbackEntity`, `SpringDataFeedbackRepository`, `FeedbackR2dbcRepository`
6. Update `TenantR2dbcRepositoryConfig` basePackages (must include both old and new during migration)

**Verify: Feedback CRUD works end-to-end.**

### Phase 3: Migrate remaining entities

Do 2-3 entities at a time. Order by complexity:
1. Simple (Company, Group, Endpoint, CounselNote, SharedFile, ScreenCapture) -- no dynamic queries
2. Medium (Channel, Recording, Notification, ChatMessage, CoBrowsingSession) -- more query methods or @Modifying
3. Complex (Agent, Tenant) -- upsert with nullable bindings, dynamic search (admin)

### Phase 4: Clean up

- Delete empty `adapter/outbound/persistence/entity/` directory
- Delete empty `adapter/outbound/persistence/springdata/` directory
- Remove any `@Profile("!test")` that was only on adapter classes (custom impls may still need it if they use DatabaseClient)
- Update test mocks to use new interface names

### Phase 5: Repeat for api-admin module

Same sequence. Can happen in parallel if different developers.

---

## Risks and Mitigations

**Risk: Spring Data R2DBC may not handle domain model constructor defaults correctly.**
- Spring Data R2DBC uses constructor-based instantiation. Default parameter values (e.g., `deleted: Boolean = false`) work correctly with Kotlin -- the framework calls the constructor with all available column values, and missing columns use defaults.
- Mitigation: Phase 2 (single entity proof of concept) validates this before committing to full migration.

**Risk: `@Id` on domain model changes Spring Data `save()` semantics.**
- `ReactiveCrudRepository.save()` does INSERT if `@Id` is null, UPDATE if non-null. Our models always have non-null UUIDs (generated at construction time), so `save()` would always attempt UPDATE.
- This is why we use `upsert()` (INSERT...ON CONFLICT) instead of `save()`. Services must call `upsert()`, never `save()`. To prevent accidental misuse, we could override `save()` to throw `UnsupportedOperationException`, but this is heavy-handed. Better approach: document the convention and rely on code review.

**Risk: Port interfaces now depend on Spring Data (framework coupling in domain layer).**
- This is the trade-off. Pure hexagonal architecture says ports should be framework-agnostic. In practice, `ReactiveCrudRepository` is a stable, well-defined interface that acts as a specification. The domain models now have Spring Data annotations, which is a similar level of coupling.
- If this is unacceptable, an alternative is to keep port interfaces pure and have them extend a marker interface, with a separate Spring Data interface extending both -- but this reintroduces the adapter layer we are trying to eliminate.
- Pragmatic recommendation: accept the coupling. The project is a Spring Boot application, not a portable library. The annotations are declarative metadata, not behavioral coupling.

**Risk: Multi-tenant routing breaks if repository package paths change.**
- Mitigation: update `@EnableR2dbcRepositories.basePackages` in Phase 2 and verify tenant routing works before proceeding.
