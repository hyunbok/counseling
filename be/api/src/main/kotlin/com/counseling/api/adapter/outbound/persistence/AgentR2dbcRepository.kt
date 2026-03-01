package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.Agent
import com.counseling.api.domain.AgentRole
import com.counseling.api.port.outbound.AgentRepository
import io.r2dbc.spi.Readable
import org.springframework.context.annotation.Profile
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Repository
@Profile("!test")
class AgentR2dbcRepository(
    private val databaseClient: DatabaseClient,
) : AgentRepository {
    override fun findByUsernameAndNotDeleted(username: String): Mono<Agent> =
        databaseClient
            .sql("SELECT * FROM agents WHERE username = :username AND deleted = FALSE")
            .bind("username", username)
            .map { row -> mapToAgent(row) }
            .one()

    override fun findByIdAndNotDeleted(id: UUID): Mono<Agent> =
        databaseClient
            .sql("SELECT * FROM agents WHERE id = :id AND deleted = FALSE")
            .bind("id", id)
            .map { row -> mapToAgent(row) }
            .one()

    override fun save(agent: Agent): Mono<Agent> =
        databaseClient
            .sql(
                """
                INSERT INTO agents (id, username, password_hash, name, role, created_at, updated_at, deleted)
                VALUES (:id, :username, :passwordHash, :name, :role, :createdAt, :updatedAt, :deleted)
                ON CONFLICT (id) DO UPDATE SET
                    password_hash = :passwordHash,
                    name = :name,
                    role = :role,
                    updated_at = :updatedAt,
                    deleted = :deleted
                """.trimIndent(),
            ).bind("id", agent.id)
            .bind("username", agent.username)
            .bind("passwordHash", agent.passwordHash)
            .bind("name", agent.name)
            .bind("role", agent.role.name)
            .bind("createdAt", agent.createdAt)
            .bind("updatedAt", agent.updatedAt)
            .bind("deleted", agent.deleted)
            .then()
            .thenReturn(agent)

    private fun mapToAgent(row: Readable): Agent =
        Agent(
            id = row.get("id", UUID::class.java)!!,
            username = row.get("username", String::class.java)!!,
            passwordHash = row.get("password_hash", String::class.java)!!,
            name = row.get("name", String::class.java)!!,
            role = AgentRole.valueOf(row.get("role", String::class.java)!!),
            createdAt = row.get("created_at", Instant::class.java)!!,
            updatedAt = row.get("updated_at", Instant::class.java)!!,
            deleted = row.get("deleted", java.lang.Boolean::class.java)!!.booleanValue(),
        )
}
