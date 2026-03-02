package com.counseling.admin.adapter.outbound.persistence

import com.counseling.admin.domain.Channel
import com.counseling.admin.domain.ChannelStatus
import com.counseling.admin.port.outbound.AdminChannelRepository
import org.springframework.context.annotation.Profile
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.Instant
import java.util.UUID

@Repository
@Profile("!test")
class AdminChannelR2dbcRepository(
    private val databaseClient: DatabaseClient,
) : AdminChannelRepository {
    override fun findAllActiveChannels(): Flux<Channel> =
        databaseClient
            .sql(
                """
                SELECT * FROM channels
                WHERE status IN ('WAITING', 'IN_PROGRESS')
                ORDER BY created_at DESC
                """.trimIndent(),
            ).map { row -> mapToChannel(row) }
            .all()

    private fun mapToChannel(row: io.r2dbc.spi.Readable): Channel =
        Channel(
            id = row.get("id", UUID::class.java)!!,
            agentId = row.get("agent_id", UUID::class.java),
            status = ChannelStatus.valueOf(row.get("status", String::class.java)!!),
            startedAt = row.get("started_at", Instant::class.java),
            endedAt = row.get("ended_at", Instant::class.java),
            createdAt = row.get("created_at", Instant::class.java)!!,
            updatedAt = row.get("updated_at", Instant::class.java)!!,
        )
}
