package com.counseling.admin.adapter.outbound.persistence

import com.counseling.admin.domain.Agent
import com.counseling.admin.domain.AgentRole
import com.counseling.admin.domain.AgentStatus
import com.counseling.admin.port.outbound.AdminAgentRepository
import org.springframework.context.annotation.Profile
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Repository
@Profile("!test")
class AdminAgentR2dbcRepository(
    private val databaseClient: DatabaseClient,
) : AdminAgentRepository {
    override fun save(agent: Agent): Mono<Agent> =
        databaseClient
            .sql(
                """
                INSERT INTO agents (id, username, password_hash, name, role, agent_status, group_id, active, created_at, updated_at, deleted)
                VALUES (:id, :username, :passwordHash, :name, :role, :agentStatus, :groupId, :active, :createdAt, :updatedAt, :deleted)
                ON CONFLICT (id) DO UPDATE SET
                    username = :username, password_hash = :passwordHash, name = :name, role = :role,
                    agent_status = :agentStatus, group_id = :groupId, active = :active,
                    updated_at = :updatedAt, deleted = :deleted
                """.trimIndent(),
            ).bind("id", agent.id)
            .bind("username", agent.username)
            .bind("passwordHash", agent.passwordHash)
            .bind("name", agent.name)
            .bind("role", agent.role.name)
            .bind("agentStatus", agent.agentStatus.name)
            .bindNullable("groupId", agent.groupId, UUID::class.java)
            .bind("active", agent.active)
            .bind("createdAt", agent.createdAt)
            .bind("updatedAt", agent.updatedAt)
            .bind("deleted", agent.deleted)
            .then()
            .thenReturn(agent)

    override fun findByIdAndNotDeleted(id: UUID): Mono<Agent> =
        databaseClient
            .sql("SELECT * FROM agents WHERE id = :id AND deleted = false")
            .bind("id", id)
            .map { row -> mapToAgent(row) }
            .one()

    override fun findByUsernameAndNotDeleted(username: String): Mono<Agent> =
        databaseClient
            .sql("SELECT * FROM agents WHERE username = :username AND deleted = false")
            .bind("username", username)
            .map { row -> mapToAgent(row) }
            .one()

    override fun findAllByNotDeleted(): Flux<Agent> =
        databaseClient
            .sql("SELECT * FROM agents WHERE deleted = false ORDER BY created_at DESC")
            .map { row -> mapToAgent(row) }
            .all()

    override fun findAllByGroupIdAndNotDeleted(groupId: UUID): Flux<Agent> =
        databaseClient
            .sql("SELECT * FROM agents WHERE group_id = :groupId AND deleted = false ORDER BY created_at DESC")
            .bind("groupId", groupId)
            .map { row -> mapToAgent(row) }
            .all()

    override fun countByGroupIdAndNotDeleted(groupId: UUID): Mono<Long> =
        databaseClient
            .sql("SELECT COUNT(*) as cnt FROM agents WHERE group_id = :groupId AND deleted = false")
            .bind("groupId", groupId)
            .map { row -> row.get("cnt", java.lang.Long::class.java)!!.toLong() }
            .one()
            .defaultIfEmpty(0L)

    private fun mapToAgent(row: io.r2dbc.spi.Readable): Agent =
        Agent(
            id = row.get("id", UUID::class.java)!!,
            username = row.get("username", String::class.java)!!,
            passwordHash = row.get("password_hash", String::class.java)!!,
            name = row.get("name", String::class.java)!!,
            role = AgentRole.valueOf(row.get("role", String::class.java)!!),
            agentStatus = AgentStatus.valueOf(row.get("agent_status", String::class.java)!!),
            groupId = row.get("group_id", UUID::class.java),
            active = row.get("active", java.lang.Boolean::class.java)?.booleanValue() ?: true,
            createdAt = row.get("created_at", Instant::class.java)!!,
            updatedAt = row.get("updated_at", Instant::class.java)!!,
            deleted = row.get("deleted", java.lang.Boolean::class.java)!!.booleanValue(),
        )

    private fun DatabaseClient.GenericExecuteSpec.bindNullable(
        name: String,
        value: Any?,
        type: Class<*>,
    ): DatabaseClient.GenericExecuteSpec =
        if (value != null) {
            this.bind(name, value)
        } else {
            this.bindNull(name, type)
        }
}
