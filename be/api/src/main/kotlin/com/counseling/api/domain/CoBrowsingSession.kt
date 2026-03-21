package com.counseling.api.domain

import java.time.Instant

data class CoBrowsingSession(
    val id: String? = null,
    val channelId: String,
    val initiatedBy: String,
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
