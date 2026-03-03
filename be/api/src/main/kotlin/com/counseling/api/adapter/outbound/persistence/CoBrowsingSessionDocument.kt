package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.CoBrowsingSession
import com.counseling.api.domain.CoBrowsingStatus
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.util.UUID

@Document(collection = "co_browsing_sessions")
@CompoundIndex(name = "idx_tenant_channel_created", def = "{'tenantId': 1, 'channelId': 1, 'createdAt': -1}")
data class CoBrowsingSessionDocument(
    @Id
    val id: String,
    val tenantId: String?,
    val channelId: String,
    val initiatedBy: String,
    val status: String,
    val startedAt: Instant?,
    val endedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deleted: Boolean = false,
) {
    fun toDomain(): CoBrowsingSession =
        CoBrowsingSession(
            id = UUID.fromString(id),
            channelId = UUID.fromString(channelId),
            initiatedBy = UUID.fromString(initiatedBy),
            status = CoBrowsingStatus.valueOf(status),
            startedAt = startedAt,
            endedAt = endedAt,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deleted = deleted,
        )

    companion object {
        fun fromDomain(
            session: CoBrowsingSession,
            tenantId: String? = null,
        ): CoBrowsingSessionDocument =
            CoBrowsingSessionDocument(
                id = session.id.toString(),
                tenantId = tenantId,
                channelId = session.channelId.toString(),
                initiatedBy = session.initiatedBy.toString(),
                status = session.status.name,
                startedAt = session.startedAt,
                endedAt = session.endedAt,
                createdAt = session.createdAt,
                updatedAt = session.updatedAt,
                deleted = session.deleted,
            )
    }
}
