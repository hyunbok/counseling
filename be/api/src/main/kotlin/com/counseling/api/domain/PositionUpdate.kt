package com.counseling.api.domain

import java.time.Instant

data class PositionUpdate(
    val entryId: String,
    val position: Long,
    val queueSize: Long,
    val channelId: String? = null,
    val timestamp: Instant = Instant.now(),
)
