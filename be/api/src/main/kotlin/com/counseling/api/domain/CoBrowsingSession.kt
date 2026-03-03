package com.counseling.api.domain

import java.time.Instant
import java.util.UUID

data class CoBrowsingSession(
    val id: UUID,
    val channelId: UUID,
    val initiatedBy: UUID,
    val status: CoBrowsingStatus,
    val startedAt: Instant?,
    val endedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deleted: Boolean = false,
) {
    fun start(): CoBrowsingSession =
        copy(
            status = CoBrowsingStatus.ACTIVE,
            startedAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    fun end(): CoBrowsingSession =
        copy(
            status = CoBrowsingStatus.ENDED,
            endedAt = Instant.now(),
            updatedAt = Instant.now(),
        )
}
