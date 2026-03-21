# Migration: Raw DatabaseClient to Spring Data R2DBC

## Architecture Decisions

### 1. Multi-tenant routing

**Decision: Do NOT use `ReactiveCrudRepository` interfaces. Use `R2dbcEntityTemplate` exclusively.**

Spring Data R2DBC's `ReactiveCrudRepository` binds to a single `ConnectionFactory` at configuration time via `@EnableR2dbcRepositories(basePackages = ..., entityOperationsRef = ...)`. Supporting two different `ConnectionFactory` instances (tenant-routed vs meta) would require two separate `@EnableR2dbcRepositories` configurations scanning different packages, which forces an artificial package split and adds complexity.

Instead, inject `R2dbcEntityTemplate` (which wraps `DatabaseClient`) into each repository adapter manually. This gives us the same type-safe entity mapping benefits without the package-scanning constraints.

- **Tenant DB repositories**: inject a primary `R2dbcEntityTemplate` built from `TenantRoutingConnectionFactory`
- **Meta DB repositories** (`TenantR2dbcRepository`, `AdminTenantR2dbcRepository`, `AdminSuperAdminR2dbcRepository`): inject a `@Qualifier("metaR2dbcEntityTemplate")` built from `metaConnectionFactory`

This approach keeps the existing `R2dbcConfig` pattern intact while gaining automatic mapping.

### 2. UPSERT (ON CONFLICT)

**Decision: Keep raw `DatabaseClient` SQL for `save()` methods. Use `R2dbcEntityTemplate` only for reads.**

Rationale:
- Every `save()` in the codebase uses `INSERT ... ON CONFLICT (id) DO UPDATE SET ...` (upsert semantics)
- `FeedbackR2dbcRepository.save()` uses `ON CONFLICT (channel_id) DO NOTHING` (idempotent insert)
- `R2dbcEntityTemplate.insert()` / `.update()` cannot express upsert or conflict clauses
- Implementing `Persistable<UUID>` with an `isNew` flag adds state-tracking complexity to domain models for no benefit, since the code never distinguishes insert vs update

Each repository adapter will hold **both** a `DatabaseClient` (for writes/upserts) and an `R2dbcEntityTemplate` (for reads). The `DatabaseClient` is obtainable from the template: `template.databaseClient`.

### 3. Dynamic search queries

**Decision: Keep `DatabaseClient` with dynamic SQL for search methods. Do NOT use `Criteria` API.**

Rationale:
- The `Criteria` API in `R2dbcEntityTemplate` does not support `LIKE` with `LOWER()` function calls, which the search methods use
- The dynamic WHERE clause builder pattern (mutableListOf + conditional adds) is already clean and well-established in the admin module
- Migrating to `Criteria` would lose the `LOWER()` LIKE pattern and require custom SQL fragments anyway

The search/count methods (`searchByNotDeleted`, `countSearchByNotDeleted`) will continue using `DatabaseClient` directly, but will share the entity-level `mapToXxx` via the entity class rather than duplicating row mapping.

### 4. Enum handling

**Decision: Register global `EnumWriteConverter` / `EnumReadConverter` pairs via `AbstractR2dbcConfiguration`.**

Spring Data R2DBC supports custom converters registered in `getCustomConverters()`. Register a generic approach:
- All enums serialize to `String` via `.name` (write converter)
- Each enum type gets a read converter from `String` via `valueOf()` (read converter)

Alternatively, since Spring Data R2DBC already converts enums to/from strings by default when the column type is VARCHAR, we can rely on the built-in behavior and only register converters if we hit edge cases. **Start with no custom converters; add them only if default behavior fails.**

### 5. Column naming

**Decision: Rely on Spring Data R2DBC's default `NamingStrategy` (camelCase to snake_case). No `@Column` annotations needed.**

Spring Data R2DBC's default naming strategy converts `passwordHash` to `password_hash`, `createdAt` to `created_at`, etc. This matches the existing DB schema exactly. No configuration required.

### 6. Domain model annotations

**Decision: Create separate R2DBC entity classes in the persistence adapter layer. Domain models stay annotation-free.**

Rationale:
- Domain models are shared across the hexagonal boundary and must remain persistence-agnostic
- R2DBC entity classes live in `adapter/outbound/persistence/entity/` and carry `@Table`, `@Id` annotations
- Each entity class has `toDomain()` and companion `fromDomain()` conversion methods
- This is consistent with the existing MongoDB document pattern (`ChannelHistoryDocument`, `ChatMessageDocument`, etc.)


## Entity Layer Design

### Package location

```
adapter/outbound/persistence/
  entity/          <-- NEW: R2DBC entity classes
    AgentEntity.kt
    ChannelEntity.kt
    ...
  AgentR2dbcRepository.kt   (existing, refactored)
  ...
```

### Entity class pattern

Each entity mirrors the domain model but adds R2DBC annotations. Example structure for `AgentEntity`:

```
@Table("agents")
data class AgentEntity(
    @Id val id: UUID,
    val username: String,
    val passwordHash: String,     // NamingStrategy -> password_hash
    val name: String,
    val role: AgentRole,          // enum -> VARCHAR via default converter
    val createdAt: Instant,
    val updatedAt: Instant,
    val deleted: Boolean,
    val groupId: UUID?,
    val agentStatus: AgentStatus,
    val email: String?,
) {
    fun toDomain(): Agent = Agent(id, username, passwordHash, name, role, ...)
    companion object {
        fun fromDomain(a: Agent): AgentEntity = AgentEntity(a.id, a.username, ...)
    }
}
```

### Entities to create (api module: 14, admin module: 5 distinct)

| Entity | Table | Notes |
|--------|-------|-------|
| `AgentEntity` | `agents` | Both modules |
| `ChannelEntity` | `channels` | Both modules |
| `ChatMessageEntity` | `chat_messages` | api only |
| `CoBrowsingSessionEntity` | `co_browsing_sessions` | api only |
| `CompanyEntity` | `companies` | Both modules |
| `CounselNoteEntity` | `counsel_notes` | api only |
| `EndpointEntity` | `endpoints` | api only |
| `FeedbackEntity` | `feedbacks` | api only |
| `GroupEntity` | `groups` | Both modules |
| `NotificationEntity` | `notifications` | api only |
| `RecordingEntity` | `recordings` | api only |
| `ScreenCaptureEntity` | `screen_captures` | api only |
| `SharedFileEntity` | `shared_files` | api only |
| `TenantEntity` | `tenants` | Both modules (meta DB) |
| `SuperAdminEntity` | `super_admins` | admin only (meta DB) |

The admin module has its own domain classes (different package), so it gets its own entity classes even where table names overlap.


## Repository Layer Design

### What changes in each repository

#### Reads: replace `DatabaseClient.sql().map().one/all()` with `R2dbcEntityTemplate`

Before:
```
databaseClient.sql("SELECT * FROM agents WHERE id = :id AND deleted = FALSE")
    .bind("id", id).map { row -> mapToAgent(row) }.one()
```

After:
```
template.select(AgentEntity::class.java)
    .matching(query(where("id").is(id).and("deleted").is(false)))
    .one()
    .map { it.toDomain() }
```

Benefits: eliminates all `mapToXxx()` functions (15+ methods across the codebase), eliminates manual `row.get()` calls with type casts.

#### Writes: keep DatabaseClient with raw SQL

No change to `save()` methods. They continue using `INSERT ... ON CONFLICT` via `template.databaseClient`.

#### Custom operations: keep DatabaseClient

`markAsRead()`, `markAllAsReadByRecipientId()`, `softDelete()` -- these are UPDATE statements that do not map to entity operations. Keep as `DatabaseClient` raw SQL.

### Repository classification

| Repository | Read approach | Write approach | DatabaseClient source |
|-----------|--------------|---------------|----------------------|
| `AgentR2dbcRepository` | `R2dbcEntityTemplate` | `DatabaseClient` (upsert) | primary (tenant) |
| `ChannelR2dbcRepository` | `R2dbcEntityTemplate` | `DatabaseClient` (upsert) | primary (tenant) |
| `ChatMessageR2dbcRepository` | `R2dbcEntityTemplate` | `DatabaseClient` (insert-only) | primary (tenant) |
| `CoBrowsingSessionR2dbcRepository` | `R2dbcEntityTemplate` | `DatabaseClient` (upsert) | primary (tenant) |
| `CompanyR2dbcRepository` | `R2dbcEntityTemplate` | `DatabaseClient` (upsert) | primary (tenant) |
| `CounselNoteR2dbcRepository` | `R2dbcEntityTemplate` | `DatabaseClient` (upsert) | primary (tenant) |
| `EndpointR2dbcRepository` | `R2dbcEntityTemplate` | `DatabaseClient` (upsert) | primary (tenant) |
| `FeedbackR2dbcRepository` | `R2dbcEntityTemplate` | `DatabaseClient` (ON CONFLICT DO NOTHING) | primary (tenant) |
| `GroupR2dbcRepository` | `R2dbcEntityTemplate` | `DatabaseClient` (upsert) | primary (tenant) |
| `NotificationR2dbcRepository` | `R2dbcEntityTemplate` | `DatabaseClient` (insert + update) | primary (tenant) |
| `RecordingR2dbcRepository` | `R2dbcEntityTemplate` | `DatabaseClient` (upsert) | primary (tenant) |
| `ScreenCaptureR2dbcRepository` | `R2dbcEntityTemplate` | `DatabaseClient` (insert + softDelete) | primary (tenant) |
| `SharedFileR2dbcRepository` | `R2dbcEntityTemplate` | `DatabaseClient` (insert + softDelete) | primary (tenant) |
| `TenantR2dbcRepository` | `R2dbcEntityTemplate` | `DatabaseClient` (upsert) | **meta** |
| `AdminAgentR2dbcRepository` | **Mixed** (template for simple reads, DatabaseClient for search) | `DatabaseClient` (upsert) | primary (tenant) |
| `AdminTenantR2dbcRepository` | **Mixed** | `DatabaseClient` (upsert) | **meta** |
| `AdminSuperAdminR2dbcRepository` | `R2dbcEntityTemplate` | none (read-only) | **meta** |
| `AdminChannelR2dbcRepository` | `R2dbcEntityTemplate` | none (read-only) | primary (tenant) |
| `AdminCompanyR2dbcRepository` | depends on methods | `DatabaseClient` (upsert) | primary (tenant) |
| `AdminGroupR2dbcRepository` | depends on methods | `DatabaseClient` (upsert) | primary (tenant) |


## Converter Design

### Default behavior (no custom converters needed)

Spring Data R2DBC handles these automatically:
- `UUID` <-> `uuid` column type
- `Instant` <-> `timestamptz` column type
- `String` <-> `varchar/text` column type
- `Boolean` <-> `boolean` column type
- `Int` / `Long` <-> `integer/bigint` column type
- **Enums** <-> `varchar` column type (uses `.name()` and `valueOf()` by default)

### Potential issue: nullable primitives

The current code uses `row.get("deleted", java.lang.Boolean::class.java)!!.booleanValue()` because R2DBC SPI returns boxed types. With `R2dbcEntityTemplate`, Kotlin's `Boolean` (non-nullable) maps correctly since the column is `NOT NULL`.

### Action: no custom converters initially

Register converters only if integration tests reveal mapping failures. The most likely candidate is `AgentStatus` default value handling (`row.get("agent_status", String::class.java) ?: AgentStatus.OFFLINE.name`), which should be handled in the entity class `toDomain()` method instead.


## Multi-tenant Strategy

### Configuration changes

#### api module: add `R2dbcEntityTemplate` beans to existing R2dbcConfig

```
// Primary template (tenant-routed)
@Bean @Primary
fun tenantR2dbcEntityTemplate(tenantConnectionFactory: ConnectionFactory): R2dbcEntityTemplate

// Meta template (for Tenant/SuperAdmin repositories)
@Bean @Qualifier("metaR2dbcEntityTemplate")
fun metaR2dbcEntityTemplate(@Qualifier("metaConnectionFactory") cf: ConnectionFactory): R2dbcEntityTemplate
```

#### admin module: same pattern in AdminR2dbcConfig

The existing `R2dbcConfig` already creates both `metaDatabaseClient` and primary `tenantDatabaseClient`. Add the corresponding `R2dbcEntityTemplate` beans alongside them.

### Injection pattern

```
class TenantR2dbcRepository(
    @Qualifier("metaR2dbcEntityTemplate") private val template: R2dbcEntityTemplate,
) : TenantRepository {
    private val client = template.databaseClient
    // reads use template, writes use client
}

class AgentR2dbcRepository(
    private val template: R2dbcEntityTemplate,  // primary, tenant-routed
) : AgentRepository {
    private val client = template.databaseClient
}
```

### Important: R2dbcEntityTemplate and TenantRoutingConnectionFactory

`R2dbcEntityTemplate` wraps a `DatabaseClient` which wraps a `ConnectionFactory`. Since `TenantRoutingConnectionFactory` resolves the tenant from `TenantContext` (reactive context) at connection-acquisition time, the template will correctly route to the right tenant DB. No special handling needed -- the reactive context propagation works the same way.


## Implementation Plan

### Phase 1: Infrastructure (no behavior change)

1. **Create entity classes** in `adapter/outbound/persistence/entity/` for both modules
   - Add `@Table`, `@Id` annotations
   - Add `toDomain()` and `fromDomain()` methods
   - Write unit tests for domain <-> entity conversion

2. **Add `R2dbcEntityTemplate` beans** to `R2dbcConfig` in both modules
   - Primary template (tenant-routed)
   - Meta template (qualified)
   - Verify beans are created correctly with integration test

### Phase 2: Migrate simple read-only repositories first

3. **Start with `AdminSuperAdminR2dbcRepository`** (simplest: read-only, single method, meta DB)
   - Replace `DatabaseClient.sql().map().one()` with `template.select().matching().one()`
   - Verify with existing tests

4. **Migrate `AdminChannelR2dbcRepository`** (read-only, tenant DB, simple queries)

### Phase 3: Migrate read methods in read-write repositories

5. **Migrate read methods in api module repositories** one at a time:
   - `AgentR2dbcRepository` (5 methods: 4 reads, 1 write)
   - `ChannelR2dbcRepository` (5 reads, 1 write)
   - `GroupR2dbcRepository`
   - `CompanyR2dbcRepository`
   - Continue through all 13 api repositories

6. **Migrate read methods in admin module repositories**
   - Skip `searchByNotDeleted` / `countSearchByNotDeleted` -- these stay as `DatabaseClient`
   - Migrate simple finds: `findById`, `findBySlug`, `findAllByDeletedFalse`, paginated variants

### Phase 4: Clean up

7. **Remove all `mapToXxx()` private functions** from repositories (replaced by entity auto-mapping)

8. **Remove `bindNull` / nullable handling boilerplate** from write methods where possible
   - The admin module already has a `bindNullable` extension; standardize this as a shared utility

9. **Delete the `@Profile("!test")` pattern** if test infrastructure is updated to use testcontainers or embedded DB (separate decision, out of scope)

### Phase 5: Verify

10. **Run full test suite** for both modules
11. **Manual smoke test** of multi-tenant routing with template-based reads

### Ordering rationale

- Entity classes first because they are a prerequisite and have no risk
- Read-only repos first because they are lowest risk (no upsert complexity)
- Api module before admin module because api has simpler query patterns
- Search methods last (or never) because they gain the least from migration

### Files to create

**api module:**
- `be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/entity/AgentEntity.kt`
- `be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/entity/ChannelEntity.kt`
- `be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/entity/ChatMessageEntity.kt`
- `be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/entity/CoBrowsingSessionEntity.kt`
- `be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/entity/CompanyEntity.kt`
- `be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/entity/CounselNoteEntity.kt`
- `be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/entity/EndpointEntity.kt`
- `be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/entity/FeedbackEntity.kt`
- `be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/entity/GroupEntity.kt`
- `be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/entity/NotificationEntity.kt`
- `be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/entity/RecordingEntity.kt`
- `be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/entity/ScreenCaptureEntity.kt`
- `be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/entity/SharedFileEntity.kt`
- `be/api/src/main/kotlin/com/counseling/api/adapter/outbound/persistence/entity/TenantEntity.kt`

**admin module:**
- `be/api-admin/src/main/kotlin/com/counseling/admin/adapter/outbound/persistence/entity/AgentEntity.kt`
- `be/api-admin/src/main/kotlin/com/counseling/admin/adapter/outbound/persistence/entity/ChannelEntity.kt`
- `be/api-admin/src/main/kotlin/com/counseling/admin/adapter/outbound/persistence/entity/CompanyEntity.kt`
- `be/api-admin/src/main/kotlin/com/counseling/admin/adapter/outbound/persistence/entity/GroupEntity.kt`
- `be/api-admin/src/main/kotlin/com/counseling/admin/adapter/outbound/persistence/entity/TenantEntity.kt`
- `be/api-admin/src/main/kotlin/com/counseling/admin/adapter/outbound/persistence/entity/SuperAdminEntity.kt`

### Files to modify

- `be/api/src/main/kotlin/com/counseling/api/config/R2dbcConfig.kt` (add template beans)
- `be/api-admin/src/main/kotlin/com/counseling/admin/config/R2dbcConfig.kt` (add template beans)
- All 13 `*R2dbcRepository.kt` files in api module (refactor reads)
- All 6 `Admin*R2dbcRepository.kt` files in admin module (refactor reads)


## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|-----------|
| `R2dbcEntityTemplate` does not propagate reactive context for tenant routing | High -- all tenant reads break | Test early in Phase 2 with a tenant-DB repository. The template delegates to `DatabaseClient` which uses the same `ConnectionFactory`, so context propagation should work identically. |
| Enum mapping fails silently (wrong value or null) | Medium -- data corruption on read | Unit test every entity's `toDomain()` with all enum values. Add a default-value safety net in entity constructors. |
| NamingStrategy mismatch for edge cases | Low -- query returns empty | Verify column name mapping in integration tests. Add `@Column("column_name")` only where NamingStrategy fails. |
| Performance regression from entity instantiation overhead | Very low | The overhead of constructing an entity then converting to domain is negligible compared to DB I/O. |


## What NOT to migrate

- **`save()` methods**: keep as raw `DatabaseClient` SQL with upsert semantics
- **`markAsRead()` / `softDelete()`**: keep as raw `DatabaseClient` UPDATE statements
- **`searchByNotDeleted()` / `countSearchByNotDeleted()`**: keep as dynamic `DatabaseClient` SQL
- **Port interfaces**: no changes at all -- the contract stays the same
- **Domain models**: no changes -- stay annotation-free
