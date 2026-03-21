package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.ScreenCapture
import com.counseling.api.port.outbound.ScreenCaptureRepository
import io.r2dbc.spi.Readable
import org.springframework.context.annotation.Profile
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Instant

@Repository
@Profile("!test")
class ScreenCaptureR2dbcRepository(
    private val databaseClient: DatabaseClient,
) : ScreenCaptureRepository {
    override fun save(capture: ScreenCapture): Mono<ScreenCapture> {
        val id = capture.id ?: throw IllegalStateException("ScreenCapture id must not be null before save")
        val spec =
            databaseClient
                .sql(
                    """
                    INSERT INTO screen_captures
                        (id, channel_id, captured_by, original_filename,
                         stored_filename, content_type, file_size, storage_path, note, created_at, deleted)
                    VALUES
                        (:id, :channelId, :capturedBy, :originalFilename,
                         :storedFilename, :contentType, :fileSize, :storagePath, :note, :createdAt, :deleted)
                    """.trimIndent(),
                ).bind("id", id)
                .bind("channelId", capture.channelId)
                .bind("capturedBy", capture.capturedBy)
                .bind("originalFilename", capture.originalFilename)
                .bind("storedFilename", capture.storedFilename)
                .bind("contentType", capture.contentType)
                .bind("fileSize", capture.fileSize)
                .bind("storagePath", capture.storagePath)
                .bind("createdAt", capture.createdAt)
                .bind("deleted", capture.deleted)
        val specWithNote =
            if (capture.note != null) {
                spec.bind("note", capture.note)
            } else {
                spec.bindNull("note", String::class.java)
            }
        return specWithNote.then().thenReturn(capture)
    }

    override fun findByIdAndNotDeleted(id: String): Mono<ScreenCapture> =
        databaseClient
            .sql(
                """
                SELECT id, channel_id, captured_by, original_filename,
                       stored_filename, content_type, file_size, storage_path, note, created_at, deleted
                FROM screen_captures
                WHERE id = :id AND deleted = FALSE
                """.trimIndent(),
            ).bind("id", id)
            .map { row -> mapToScreenCapture(row) }
            .one()

    override fun softDelete(id: String): Mono<Void> =
        databaseClient
            .sql("UPDATE screen_captures SET deleted = TRUE WHERE id = :id")
            .bind("id", id)
            .then()

    private fun mapToScreenCapture(row: Readable): ScreenCapture =
        ScreenCapture(
            id = row.get("id", String::class.java)!!,
            channelId = row.get("channel_id", String::class.java)!!,
            capturedBy = row.get("captured_by", String::class.java)!!,
            originalFilename = row.get("original_filename", String::class.java)!!,
            storedFilename = row.get("stored_filename", String::class.java)!!,
            contentType = row.get("content_type", String::class.java)!!,
            fileSize = row.get("file_size", Long::class.java)!!,
            storagePath = row.get("storage_path", String::class.java)!!,
            note = row.get("note", String::class.java),
            createdAt = row.get("created_at", Instant::class.java)!!,
            deleted = row.get("deleted", Boolean::class.java)!!,
        )
}
