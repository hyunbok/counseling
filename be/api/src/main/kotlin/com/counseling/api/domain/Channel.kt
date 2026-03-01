package com.counseling.api.domain

import java.time.Instant
import java.util.UUID

data class Channel(
    val id: UUID,
    val agentId: UUID?,
    val status: ChannelStatus,
    val startedAt: Instant?,
    val endedAt: Instant?,
    val recordingPath: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deleted: Boolean = false,
) {
    fun assignAgent(agentId: UUID): Channel = copy(agentId = agentId, updatedAt = Instant.now())

    fun start(): Channel =
        copy(
            status = ChannelStatus.IN_PROGRESS,
            startedAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    fun close(): Channel =
        copy(
            status = ChannelStatus.CLOSED,
            endedAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    fun isOpen(): Boolean = !deleted && status != ChannelStatus.CLOSED
}
