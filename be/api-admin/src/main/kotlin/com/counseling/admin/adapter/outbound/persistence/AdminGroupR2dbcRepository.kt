package com.counseling.admin.adapter.outbound.persistence

import com.counseling.admin.domain.Group
import com.counseling.admin.domain.GroupStatus
import com.counseling.admin.port.outbound.AdminGroupRepository
import io.r2dbc.spi.Readable
import org.springframework.context.annotation.Profile
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Repository
@Profile("!test")
class AdminGroupR2dbcRepository(
    private val databaseClient: DatabaseClient,
) : AdminGroupRepository {
    override fun save(group: Group): Mono<Group> =
        databaseClient
            .sql(
                """
                INSERT INTO groups (id, name, status, created_at, updated_at, deleted)
                VALUES (:id, :name, :status, :createdAt, :updatedAt, :deleted)
                ON CONFLICT (id) DO UPDATE SET
                    name = :name,
                    status = :status,
                    updated_at = :updatedAt,
                    deleted = :deleted
                """.trimIndent(),
            ).bind("id", group.id)
            .bind("name", group.name)
            .bind("status", group.status.name)
            .bind("createdAt", group.createdAt)
            .bind("updatedAt", group.updatedAt)
            .bind("deleted", group.deleted)
            .then()
            .thenReturn(group)

    override fun findByIdAndNotDeleted(id: UUID): Mono<Group> =
        databaseClient
            .sql("SELECT * FROM groups WHERE id = :id AND deleted = FALSE")
            .bind("id", id)
            .map { row -> mapToGroup(row) }
            .one()

    override fun findAllByNotDeleted(): Flux<Group> =
        databaseClient
            .sql("SELECT * FROM groups WHERE deleted = FALSE ORDER BY created_at")
            .map { row -> mapToGroup(row) }
            .all()

    override fun findAllByNotDeleted(
        page: Int,
        size: Int,
    ): Flux<Group> =
        databaseClient
            .sql("SELECT * FROM groups WHERE deleted = FALSE ORDER BY created_at LIMIT :limit OFFSET :offset")
            .bind("limit", size)
            .bind("offset", page * size)
            .map { row -> mapToGroup(row) }
            .all()

    override fun countAllByNotDeleted(): Mono<Long> =
        databaseClient
            .sql("SELECT COUNT(*) as cnt FROM groups WHERE deleted = FALSE")
            .map { row -> row.get("cnt", java.lang.Long::class.java)!!.toLong() }
            .one()
            .defaultIfEmpty(0L)

    override fun findByNameAndNotDeleted(name: String): Mono<Group> =
        databaseClient
            .sql("SELECT * FROM groups WHERE name = :name AND deleted = FALSE")
            .bind("name", name)
            .map { row -> mapToGroup(row) }
            .one()

    private fun mapToGroup(row: Readable): Group =
        Group(
            id = row.get("id", UUID::class.java)!!,
            name = row.get("name", String::class.java)!!,
            status = GroupStatus.valueOf(row.get("status", String::class.java)!!),
            createdAt = row.get("created_at", Instant::class.java)!!,
            updatedAt = row.get("updated_at", Instant::class.java)!!,
            deleted = row.get("deleted", java.lang.Boolean::class.java)!!.booleanValue(),
        )
}
