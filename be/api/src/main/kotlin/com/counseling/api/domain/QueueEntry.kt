package com.counseling.api.domain

import java.time.Instant
import java.util.UUID

data class QueueEntry(
    val id: UUID,
    val customerName: String,
    val customerContact: String,
    val groupId: UUID?,
    val enteredAt: Instant,
    val userAgent: String? = null,
)
