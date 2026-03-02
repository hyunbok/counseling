package com.counseling.api.domain

import java.time.Instant
import java.util.UUID

data class PositionUpdate(
    val entryId: UUID,
    val position: Long,
    val queueSize: Long,
    val channelId: UUID? = null,
    val timestamp: Instant = Instant.now(),
)
