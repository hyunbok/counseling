package com.counseling.admin.adapter.outbound.persistence

import com.counseling.admin.port.inbound.AgentStats
import com.counseling.admin.port.inbound.StatsSummary
import com.counseling.admin.port.outbound.AdminStatsRepository
import org.springframework.context.annotation.Profile
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Repository
@Profile("!test")
class AdminStatsR2dbcRepository(
    private val databaseClient: DatabaseClient,
) : AdminStatsRepository {
    override fun getSummary(
        from: Instant,
        to: Instant,
    ): Mono<StatsSummary> =
        databaseClient
            .sql(
                """
                SELECT
                    COUNT(*) as total_channels,
                    COUNT(*) FILTER (WHERE status = 'CLOSED') as completed_channels,
                    COALESCE(AVG(f.rating), 0) as avg_rating,
                    COALESCE(AVG(EXTRACT(EPOCH FROM (c.ended_at - c.started_at)))::BIGINT, 0) as avg_handle_time
                FROM channels c
                LEFT JOIN feedbacks f ON f.channel_id = c.id
                WHERE c.created_at >= :from AND c.created_at < :to
                """.trimIndent(),
            ).bind("from", from)
            .bind("to", to)
            .map { row ->
                StatsSummary(
                    totalChannels = row.get("total_channels", java.lang.Long::class.java)?.toLong() ?: 0L,
                    completedChannels = row.get("completed_channels", java.lang.Long::class.java)?.toLong() ?: 0L,
                    averageRating = row.get("avg_rating", java.math.BigDecimal::class.java)?.toDouble() ?: 0.0,
                    averageHandleTimeSeconds = row.get("avg_handle_time", java.lang.Long::class.java)?.toLong() ?: 0L,
                )
            }.one()
            .defaultIfEmpty(StatsSummary(0L, 0L, 0.0, 0L))

    override fun getAgentStats(
        from: Instant,
        to: Instant,
    ): Mono<List<AgentStats>> =
        databaseClient
            .sql(
                """
                SELECT
                    a.id as agent_id,
                    a.name as agent_name,
                    COUNT(c.id) as total_channels,
                    COUNT(c.id) FILTER (WHERE c.status = 'CLOSED') as completed_channels,
                    COALESCE(AVG(f.rating), 0) as avg_rating,
                    COALESCE(AVG(EXTRACT(EPOCH FROM (c.ended_at - c.started_at)))::BIGINT, 0) as avg_handle_time
                FROM agents a
                LEFT JOIN channels c ON c.agent_id = a.id AND c.created_at >= :from AND c.created_at < :to
                LEFT JOIN feedbacks f ON f.channel_id = c.id
                WHERE a.deleted = false
                GROUP BY a.id, a.name
                ORDER BY total_channels DESC
                """.trimIndent(),
            ).bind("from", from)
            .bind("to", to)
            .map { row ->
                AgentStats(
                    agentId = row.get("agent_id", UUID::class.java)!!,
                    agentName = row.get("agent_name", String::class.java)!!,
                    totalChannels = row.get("total_channels", java.lang.Long::class.java)?.toLong() ?: 0L,
                    completedChannels = row.get("completed_channels", java.lang.Long::class.java)?.toLong() ?: 0L,
                    averageRating = row.get("avg_rating", java.math.BigDecimal::class.java)?.toDouble() ?: 0.0,
                    averageHandleTimeSeconds = row.get("avg_handle_time", java.lang.Long::class.java)?.toLong() ?: 0L,
                )
            }.all()
            .collectList()
}
