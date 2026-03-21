package com.counseling.api.domain

import java.time.Instant

data class QueueEntry(
    val id: String,
    val customerName: String,
    val customerContact: String,
    val groupId: String?,
    val enteredAt: Instant,
)
