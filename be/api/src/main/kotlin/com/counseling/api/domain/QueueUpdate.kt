package com.counseling.api.domain

import java.time.Instant

data class QueueUpdate(
    val type: QueueUpdateType,
    val entry: QueueEntry?,
    val queueSize: Long,
    val timestamp: Instant = Instant.now(),
)
