package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.Channel
import com.counseling.api.domain.ChannelStatus
import com.counseling.api.port.outbound.ChannelRepository
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
class ChannelR2dbcRepository(
    private val databaseClient: DatabaseClient,
) : ChannelRepository {
    override fun save(channel: Channel): Mono<Channel> {
        val spec =
            databaseClient
                .sql(
                    """
                    INSERT INTO channels (id, agent_id, status, started_at, ended_at, recording_path, livekit_room_name, created_at, updated_at, deleted)
                    VALUES (:id, :agentId, :status, :startedAt, :endedAt, :recordingPath, :livekitRoomName, :createdAt, :updatedAt, :deleted)
                    ON CONFLICT (id) DO UPDATE SET
                        agent_id = :agentId,
                        status = :status,
                        started_at = :startedAt,
                        ended_at = :endedAt,
                        recording_path = :recordingPath,
                        livekit_room_name = :livekitRoomName,
                        updated_at = :updatedAt,
                        deleted = :deleted
                    """.trimIndent(),
                ).bind("id", channel.id)
                .bind("status", channel.status.name)
                .bind("createdAt", channel.createdAt)
                .bind("updatedAt", channel.updatedAt)
                .bind("deleted", channel.deleted)
        val specWithAgent =
            if (channel.agentId != null) {
                spec.bind("agentId", channel.agentId)
            } else {
                spec.bindNull("agentId", UUID::class.java)
            }
        val specWithStartedAt =
            if (channel.startedAt != null) {
                specWithAgent.bind("startedAt", channel.startedAt)
            } else {
                specWithAgent.bindNull("startedAt", Instant::class.java)
            }
        val specWithEndedAt =
            if (channel.endedAt != null) {
                specWithStartedAt.bind("endedAt", channel.endedAt)
            } else {
                specWithStartedAt.bindNull("endedAt", Instant::class.java)
            }
        val specWithRecording =
            if (channel.recordingPath != null) {
                specWithEndedAt.bind("recordingPath", channel.recordingPath)
            } else {
                specWithEndedAt.bindNull("recordingPath", String::class.java)
            }
        val specWithRoomName =
            if (channel.livekitRoomName != null) {
                specWithRecording.bind("livekitRoomName", channel.livekitRoomName)
            } else {
                specWithRecording.bindNull("livekitRoomName", String::class.java)
            }
        return specWithRoomName.then().thenReturn(channel)
    }

    override fun findByIdAndNotDeleted(id: UUID): Mono<Channel> =
        databaseClient
            .sql("SELECT * FROM channels WHERE id = :id AND deleted = FALSE")
            .bind("id", id)
            .map { row -> mapToChannel(row) }
            .one()

    override fun findAllByAgentIdAndNotDeleted(agentId: UUID): Flux<Channel> =
        databaseClient
            .sql("SELECT * FROM channels WHERE agent_id = :agentId AND deleted = FALSE ORDER BY created_at")
            .bind("agentId", agentId)
            .map { row -> mapToChannel(row) }
            .all()

    override fun findAllByStatusAndNotDeleted(status: ChannelStatus): Flux<Channel> =
        databaseClient
            .sql("SELECT * FROM channels WHERE status = :status AND deleted = FALSE ORDER BY created_at")
            .bind("status", status.name)
            .map { row -> mapToChannel(row) }
            .all()

    override fun findAllByAgentIdAndStatusAndNotDeleted(
        agentId: UUID,
        status: ChannelStatus,
    ): Flux<Channel> =
        databaseClient
            .sql(
                "SELECT * FROM channels WHERE agent_id = :agentId AND status = :status AND deleted = FALSE ORDER BY created_at",
            ).bind("agentId", agentId)
            .bind("status", status.name)
            .map { row -> mapToChannel(row) }
            .all()

    override fun findByLivekitRoomNameAndNotDeleted(roomName: String): Mono<Channel> =
        databaseClient
            .sql("SELECT * FROM channels WHERE livekit_room_name = :roomName AND deleted = FALSE")
            .bind("roomName", roomName)
            .map { row -> mapToChannel(row) }
            .one()

    private fun mapToChannel(row: Readable): Channel =
        Channel(
            id = row.get("id", UUID::class.java)!!,
            agentId = row.get("agent_id", UUID::class.java),
            status = ChannelStatus.valueOf(row.get("status", String::class.java)!!),
            startedAt = row.get("started_at", Instant::class.java),
            endedAt = row.get("ended_at", Instant::class.java),
            recordingPath = row.get("recording_path", String::class.java),
            livekitRoomName = row.get("livekit_room_name", String::class.java),
            createdAt = row.get("created_at", Instant::class.java)!!,
            updatedAt = row.get("updated_at", Instant::class.java)!!,
            deleted = row.get("deleted", Boolean::class.java)!!,
        )
}
