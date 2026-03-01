package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.Agent
import com.counseling.api.domain.AgentRole
import com.counseling.api.domain.AgentStatus
import com.counseling.api.port.outbound.AgentRepository
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

    override fun save(agent: Agent): Mono<Agent> {
        val spec =
            databaseClient
                .sql(
                    """
                    INSERT INTO agents (id, username, password_hash, name, role, created_at, updated_at, deleted, group_id, agent_status)
                    VALUES (:id, :username, :passwordHash, :name, :role, :createdAt, :updatedAt, :deleted, :groupId, :agentStatus)
                    ON CONFLICT (id) DO UPDATE SET
                        password_hash = :passwordHash,
                        name = :name,
                        role = :role,
                        updated_at = :updatedAt,
                        deleted = :deleted,
                        group_id = :groupId,
                        agent_status = :agentStatus
                    """.trimIndent(),
                ).bind("id", agent.id)
                .bind("username", agent.username)
                .bind("passwordHash", agent.passwordHash)
                .bind("name", agent.name)
                .bind("role", agent.role.name)
                .bind("createdAt", agent.createdAt)
                .bind("updatedAt", agent.updatedAt)
                .bind("deleted", agent.deleted)
                .bind("agentStatus", agent.agentStatus.name)
        val specWithGroup =
            if (agent.groupId != null) {
                spec.bind("groupId", agent.groupId)
            } else {
                spec.bindNull("groupId", UUID::class.java)
            }
        return specWithGroup.then().thenReturn(agent)
    }

    override fun findAllByGroupIdAndNotDeleted(groupId: UUID): Flux<Agent> =
        databaseClient
            .sql("SELECT * FROM agents WHERE group_id = :groupId AND deleted = FALSE")
            .bind("groupId", groupId)
            .map { row -> mapToAgent(row) }
            .all()

    override fun findAllByNotDeleted(): Flux<Agent> =
        databaseClient
            .sql("SELECT * FROM agents WHERE deleted = FALSE")
            .map { row -> mapToAgent(row) }
            .all()

    private fun mapToAgent(row: Readable): Agent =
        Agent(
            id = row.get("id", UUID::class.java)!!,
            username = row.get("username", String::class.java)!!,
            passwordHash = row.get("password_hash", String::class.java)!!,
            name = row.get("name", String::class.java)!!,
            role = AgentRole.valueOf(row.get("role", String::class.java)!!),
            createdAt = row.get("created_at", Instant::class.java)!!,
            updatedAt = row.get("updated_at", Instant::class.java)!!,
            deleted = row.get("deleted", Boolean::class.java)!!,
            groupId = row.get("group_id", UUID::class.java),
            agentStatus = AgentStatus.valueOf(
                row.get("agent_status", String::class.java) ?: AgentStatus.OFFLINE.name,
            ),
        )
}
