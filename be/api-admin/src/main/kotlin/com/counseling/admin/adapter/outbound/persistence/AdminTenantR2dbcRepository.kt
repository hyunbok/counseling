package com.counseling.admin.adapter.outbound.persistence

import com.counseling.admin.domain.Tenant
import com.counseling.admin.domain.TenantStatus
import com.counseling.admin.port.outbound.AdminTenantRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Repository
@Profile("!test")
class AdminTenantR2dbcRepository(
    @Qualifier("metaDatabaseClient") private val databaseClient: DatabaseClient,
) : AdminTenantRepository {
    override fun save(tenant: Tenant): Mono<Tenant> =
        databaseClient
            .sql(
                """
                INSERT INTO tenants (id, name, slug, status, db_host, db_port, db_name, db_username, db_password, created_at, updated_at, deleted)
                VALUES (:id, :name, :slug, :status, :dbHost, :dbPort, :dbName, :dbUsername, :dbPassword, :createdAt, :updatedAt, :deleted)
                ON CONFLICT (id) DO UPDATE SET
                    name = :name, slug = :slug, status = :status,
                    db_host = :dbHost, db_port = :dbPort, db_name = :dbName,
                    db_username = :dbUsername, db_password = :dbPassword,
                    updated_at = :updatedAt, deleted = :deleted
                """.trimIndent(),
            ).bind("id", tenant.id)
            .bind("name", tenant.name)
            .bind("slug", tenant.slug)
            .bind("status", tenant.status.name)
            .bind("dbHost", tenant.dbHost)
            .bind("dbPort", tenant.dbPort)
            .bind("dbName", tenant.dbName)
            .bind("dbUsername", tenant.dbUsername)
            .bind("dbPassword", tenant.dbPassword)
            .bind("createdAt", tenant.createdAt)
            .bind("updatedAt", tenant.updatedAt)
            .bind("deleted", tenant.deleted)
            .then()
            .thenReturn(tenant)

    override fun findById(id: UUID): Mono<Tenant> =
        databaseClient
            .sql("SELECT * FROM tenants WHERE id = :id AND deleted = false")
            .bind("id", id)
            .map { row -> mapToTenant(row) }
            .one()

    override fun findBySlug(slug: String): Mono<Tenant> =
        databaseClient
            .sql("SELECT * FROM tenants WHERE slug = :slug AND deleted = false")
            .bind("slug", slug)
            .map { row -> mapToTenant(row) }
            .one()

    override fun findAllByDeletedFalse(
        page: Int,
        size: Int,
    ): Flux<Tenant> =
        databaseClient
            .sql("SELECT * FROM tenants WHERE deleted = false ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
            .bind("limit", size)
            .bind("offset", page * size)
            .map { row -> mapToTenant(row) }
            .all()

    override fun countAllByDeletedFalse(): Mono<Long> =
        databaseClient
            .sql("SELECT COUNT(*) as cnt FROM tenants WHERE deleted = false")
            .map { row -> row.get("cnt", java.lang.Long::class.java)!!.toLong() }
            .one()
            .defaultIfEmpty(0L)

    override fun findAllByStatusAndDeletedFalse(status: String): Flux<Tenant> =
        databaseClient
            .sql("SELECT * FROM tenants WHERE status = :status AND deleted = false ORDER BY created_at DESC")
            .bind("status", status)
            .map { row -> mapToTenant(row) }
            .all()

    override fun findAllByStatusAndDeletedFalse(
        status: String,
        page: Int,
        size: Int,
    ): Flux<Tenant> =
        databaseClient
            .sql(
                """
                SELECT * FROM tenants
                WHERE status = :status AND deleted = false
                ORDER BY created_at DESC LIMIT :limit OFFSET :offset
                """.trimIndent(),
            ).bind("status", status)
            .bind("limit", size)
            .bind("offset", page * size)
            .map { row -> mapToTenant(row) }
            .all()

    override fun countAllByStatusAndDeletedFalse(status: String): Mono<Long> =
        databaseClient
            .sql("SELECT COUNT(*) as cnt FROM tenants WHERE status = :status AND deleted = false")
            .bind("status", status)
            .map { row -> row.get("cnt", java.lang.Long::class.java)!!.toLong() }
            .one()
            .defaultIfEmpty(0L)

    private fun mapToTenant(row: io.r2dbc.spi.Readable): Tenant =
        Tenant(
            id = row.get("id", UUID::class.java)!!,
            name = row.get("name", String::class.java)!!,
            slug = row.get("slug", String::class.java)!!,
            status = TenantStatus.valueOf(row.get("status", String::class.java)!!),
            dbHost = row.get("db_host", String::class.java)!!,
            dbPort = row.get("db_port", Integer::class.java)!!.toInt(),
            dbName = row.get("db_name", String::class.java)!!,
            dbUsername = row.get("db_username", String::class.java)!!,
            dbPassword = row.get("db_password", String::class.java)!!,
            createdAt = row.get("created_at", Instant::class.java)!!,
            updatedAt = row.get("updated_at", Instant::class.java)!!,
            deleted = row.get("deleted", java.lang.Boolean::class.java)!!.booleanValue(),
        )
}
