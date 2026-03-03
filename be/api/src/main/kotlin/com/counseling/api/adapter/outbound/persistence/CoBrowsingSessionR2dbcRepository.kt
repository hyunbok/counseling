package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.CoBrowsingSession
import com.counseling.api.domain.CoBrowsingStatus
import com.counseling.api.port.outbound.CoBrowsingSessionRepository
import io.r2dbc.spi.Readable
import org.springframework.context.annotation.Profile
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Repository
@Profile("!test")
class CoBrowsingSessionR2dbcRepository(
    private val databaseClient: DatabaseClient,
) : CoBrowsingSessionRepository {
    override fun save(session: CoBrowsingSession): Mono<CoBrowsingSession> {
        val spec =
            databaseClient
                .sql(
                    """
                    INSERT INTO co_browsing_sessions
                        (id, channel_id, initiated_by, status, started_at, ended_at, created_at, updated_at, deleted)
                    VALUES
                        (:id, :channelId, :initiatedBy, :status, :startedAt, :endedAt, :createdAt, :updatedAt, :deleted)
                    ON CONFLICT (id) DO UPDATE SET
                        status = EXCLUDED.status,
                        started_at = EXCLUDED.started_at,
                        ended_at = EXCLUDED.ended_at,
                        updated_at = EXCLUDED.updated_at,
                        deleted = EXCLUDED.deleted
                    """.trimIndent(),
                ).bind("id", session.id)
                .bind("channelId", session.channelId)
                .bind("initiatedBy", session.initiatedBy)
                .bind("status", session.status.name)
                .bind("createdAt", session.createdAt)
                .bind("updatedAt", session.updatedAt)
                .bind("deleted", session.deleted)
        val specWithStartedAt =
            if (session.startedAt != null) {
                spec.bind("startedAt", session.startedAt)
            } else {
                spec.bindNull("startedAt", Instant::class.java)
            }
        val specWithEndedAt =
            if (session.endedAt != null) {
                specWithStartedAt.bind("endedAt", session.endedAt)
            } else {
                specWithStartedAt.bindNull("endedAt", Instant::class.java)
            }
        return specWithEndedAt.then().thenReturn(session)
    }

    override fun findByIdAndNotDeleted(id: UUID): Mono<CoBrowsingSession> =
        databaseClient
            .sql(
                """
                SELECT id, channel_id, initiated_by, status, started_at, ended_at, created_at, updated_at, deleted
                FROM co_browsing_sessions
                WHERE id = :id AND deleted = FALSE
                """.trimIndent(),
            ).bind("id", id)
            .map { row -> mapToCoBrowsingSession(row) }
            .one()

    override fun findActiveByChannelId(channelId: UUID): Mono<CoBrowsingSession> =
        databaseClient
            .sql(
                """
                SELECT id, channel_id, initiated_by, status, started_at, ended_at, created_at, updated_at, deleted
                FROM co_browsing_sessions
                WHERE channel_id = :channelId AND deleted = FALSE AND status IN ('REQUESTED', 'ACTIVE')
                """.trimIndent(),
            ).bind("channelId", channelId)
            .map { row -> mapToCoBrowsingSession(row) }
            .one()

    private fun mapToCoBrowsingSession(row: Readable): CoBrowsingSession =
        CoBrowsingSession(
            id = row.get("id", UUID::class.java)!!,
            channelId = row.get("channel_id", UUID::class.java)!!,
            initiatedBy = row.get("initiated_by", UUID::class.java)!!,
            status = CoBrowsingStatus.valueOf(row.get("status", String::class.java)!!),
            startedAt = row.get("started_at", Instant::class.java),
            endedAt = row.get("ended_at", Instant::class.java),
            createdAt = row.get("created_at", Instant::class.java)!!,
            updatedAt = row.get("updated_at", Instant::class.java)!!,
            deleted = row.get("deleted", Boolean::class.java)!!,
        )
}
