package com.counseling.admin.adapter.outbound.persistence

import com.counseling.admin.domain.SuperAdmin
import com.counseling.admin.port.outbound.AdminSuperAdminRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Repository
@Profile("!test")
class AdminSuperAdminR2dbcRepository(
    @Qualifier("metaDatabaseClient") private val databaseClient: DatabaseClient,
) : AdminSuperAdminRepository {
    override fun findByUsernameAndNotDeleted(username: String): Mono<SuperAdmin> =
        databaseClient
            .sql("SELECT * FROM super_admins WHERE username = :username AND deleted = false")
            .bind("username", username)
            .map { row ->
                SuperAdmin(
                    id = row.get("id", UUID::class.java)!!,
                    username = row.get("username", String::class.java)!!,
                    passwordHash = row.get("password_hash", String::class.java)!!,
                    createdAt = row.get("created_at", Instant::class.java)!!,
                    updatedAt = row.get("updated_at", Instant::class.java)!!,
                    deleted = row.get("deleted", java.lang.Boolean::class.java)!!.booleanValue(),
                )
            }.one()
}
