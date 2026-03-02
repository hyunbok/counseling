package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.Recording
import com.counseling.api.domain.RecordingStatus
import com.counseling.api.port.outbound.RecordingRepository
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
class RecordingR2dbcRepository(
    private val databaseClient: DatabaseClient,
) : RecordingRepository {
    override fun save(recording: Recording): Mono<Recording> {
        val spec =
            databaseClient
                .sql(
                    """
                    INSERT INTO recordings (id, channel_id, egress_id, status, file_path, started_at, stopped_at, created_at, updated_at, deleted)
                    VALUES (:id, :channelId, :egressId, :status, :filePath, :startedAt, :stoppedAt, :createdAt, :updatedAt, :deleted)
                    ON CONFLICT (id) DO UPDATE SET
                        egress_id = :egressId,
                        status = :status,
                        file_path = :filePath,
                        stopped_at = :stoppedAt,
                        updated_at = :updatedAt,
                        deleted = :deleted
                    """.trimIndent(),
                ).bind("id", recording.id)
                .bind("channelId", recording.channelId)
                .bind("egressId", recording.egressId)
                .bind("status", recording.status.name)
                .bind("startedAt", recording.startedAt)
                .bind("createdAt", recording.createdAt)
                .bind("updatedAt", recording.updatedAt)
                .bind("deleted", recording.deleted)
        val specWithFilePath =
            if (recording.filePath != null) {
                spec.bind("filePath", recording.filePath)
            } else {
                spec.bindNull("filePath", String::class.java)
            }
        val specWithStoppedAt =
            if (recording.stoppedAt != null) {
                specWithFilePath.bind("stoppedAt", recording.stoppedAt)
            } else {
                specWithFilePath.bindNull("stoppedAt", Instant::class.java)
            }
        return specWithStoppedAt.then().thenReturn(recording)
    }

    override fun findByIdAndNotDeleted(id: UUID): Mono<Recording> =
        databaseClient
            .sql("SELECT * FROM recordings WHERE id = :id AND deleted = FALSE")
            .bind("id", id)
            .map { row -> mapToRecording(row) }
            .one()

    override fun findActiveByChannelId(channelId: UUID): Mono<Recording> =
        databaseClient
            .sql(
                "SELECT * FROM recordings WHERE channel_id = :channelId AND status = :status AND deleted = FALSE",
            ).bind("channelId", channelId)
            .bind("status", RecordingStatus.RECORDING.name)
            .map { row -> mapToRecording(row) }
            .one()

    override fun findAllByChannelIdAndNotDeleted(channelId: UUID): Flux<Recording> =
        databaseClient
            .sql(
                "SELECT * FROM recordings WHERE channel_id = :channelId AND deleted = FALSE ORDER BY created_at",
            ).bind("channelId", channelId)
            .map { row -> mapToRecording(row) }
            .all()

    private fun mapToRecording(row: Readable): Recording =
        Recording(
            id = row.get("id", UUID::class.java)!!,
            channelId = row.get("channel_id", UUID::class.java)!!,
            egressId = row.get("egress_id", String::class.java)!!,
            status = RecordingStatus.valueOf(row.get("status", String::class.java)!!),
            filePath = row.get("file_path", String::class.java),
            startedAt = row.get("started_at", Instant::class.java)!!,
            stoppedAt = row.get("stopped_at", Instant::class.java),
            createdAt = row.get("created_at", Instant::class.java)!!,
            updatedAt = row.get("updated_at", Instant::class.java)!!,
            deleted = row.get("deleted", Boolean::class.java)!!,
        )
}
