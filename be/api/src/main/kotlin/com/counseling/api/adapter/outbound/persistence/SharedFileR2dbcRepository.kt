package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.SenderType
import com.counseling.api.domain.SharedFile
import com.counseling.api.port.outbound.SharedFileRepository
import io.r2dbc.spi.Readable
import org.springframework.context.annotation.Profile
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Repository
@Profile("!test")
class SharedFileR2dbcRepository(
    private val databaseClient: DatabaseClient,
) : SharedFileRepository {
    override fun save(file: SharedFile): Mono<SharedFile> =
        databaseClient
            .sql(
                """
                INSERT INTO shared_files
                    (id, channel_id, uploader_id, uploader_type, original_filename,
                     stored_filename, content_type, file_size, storage_path, created_at, deleted)
                VALUES
                    (:id, :channelId, :uploaderId, :uploaderType, :originalFilename,
                     :storedFilename, :contentType, :fileSize, :storagePath, :createdAt, :deleted)
                """.trimIndent(),
            ).bind("id", file.id)
            .bind("channelId", file.channelId)
            .bind("uploaderId", file.uploaderId)
            .bind("uploaderType", file.uploaderType.name)
            .bind("originalFilename", file.originalFilename)
            .bind("storedFilename", file.storedFilename)
            .bind("contentType", file.contentType)
            .bind("fileSize", file.fileSize)
            .bind("storagePath", file.storagePath)
            .bind("createdAt", file.createdAt)
            .bind("deleted", file.deleted)
            .then()
            .thenReturn(file)

    override fun findByIdAndNotDeleted(id: UUID): Mono<SharedFile> =
        databaseClient
            .sql(
                """
                SELECT id, channel_id, uploader_id, uploader_type, original_filename,
                       stored_filename, content_type, file_size, storage_path, created_at, deleted
                FROM shared_files
                WHERE id = :id AND deleted = FALSE
                """.trimIndent(),
            ).bind("id", id)
            .map { row -> mapToSharedFile(row) }
            .one()

    override fun softDelete(id: UUID): Mono<Void> =
        databaseClient
            .sql("UPDATE shared_files SET deleted = TRUE WHERE id = :id")
            .bind("id", id)
            .then()

    private fun mapToSharedFile(row: Readable): SharedFile =
        SharedFile(
            id = row.get("id", UUID::class.java)!!,
            channelId = row.get("channel_id", UUID::class.java)!!,
            uploaderId = row.get("uploader_id", String::class.java)!!,
            uploaderType = SenderType.valueOf(row.get("uploader_type", String::class.java)!!),
            originalFilename = row.get("original_filename", String::class.java)!!,
            storedFilename = row.get("stored_filename", String::class.java)!!,
            contentType = row.get("content_type", String::class.java)!!,
            fileSize = row.get("file_size", Long::class.java)!!,
            storagePath = row.get("storage_path", String::class.java)!!,
            createdAt = row.get("created_at", Instant::class.java)!!,
            deleted = row.get("deleted", Boolean::class.java)!!,
        )
}
