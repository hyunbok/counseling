package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.CounselNote
import com.counseling.api.port.outbound.CounselNoteRepository
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
class CounselNoteR2dbcRepository(
    private val databaseClient: DatabaseClient,
) : CounselNoteRepository {
    override fun save(note: CounselNote): Mono<CounselNote> =
        databaseClient
            .sql(
                """
                INSERT INTO counsel_notes (id, channel_id, agent_id, content, created_at, updated_at, deleted)
                VALUES (:id, :channelId, :agentId, :content, :createdAt, :updatedAt, :deleted)
                ON CONFLICT (id) DO UPDATE SET
                    content = :content,
                    updated_at = :updatedAt,
                    deleted = :deleted
                """.trimIndent(),
            ).bind("id", note.id)
            .bind("channelId", note.channelId)
            .bind("agentId", note.agentId)
            .bind("content", note.content)
            .bind("createdAt", note.createdAt)
            .bind("updatedAt", note.updatedAt)
            .bind("deleted", note.deleted)
            .then()
            .thenReturn(note)

    override fun findByIdAndNotDeleted(id: UUID): Mono<CounselNote> =
        databaseClient
            .sql("SELECT * FROM counsel_notes WHERE id = :id AND deleted = FALSE")
            .bind("id", id)
            .map { row -> mapToCounselNote(row) }
            .one()

    override fun findAllByChannelIdAndNotDeleted(channelId: UUID): Flux<CounselNote> =
        databaseClient
            .sql("SELECT * FROM counsel_notes WHERE channel_id = :channelId AND deleted = FALSE ORDER BY created_at")
            .bind("channelId", channelId)
            .map { row -> mapToCounselNote(row) }
            .all()

    private fun mapToCounselNote(row: Readable): CounselNote =
        CounselNote(
            id = row.get("id", UUID::class.java)!!,
            channelId = row.get("channel_id", UUID::class.java)!!,
            agentId = row.get("agent_id", UUID::class.java)!!,
            content = row.get("content", String::class.java)!!,
            createdAt = row.get("created_at", Instant::class.java)!!,
            updatedAt = row.get("updated_at", Instant::class.java)!!,
            deleted = row.get("deleted", Boolean::class.java)!!,
        )
}
