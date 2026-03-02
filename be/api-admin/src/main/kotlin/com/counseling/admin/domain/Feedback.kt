package com.counseling.admin.domain

import java.time.Instant
import java.util.UUID

data class Feedback(
    val id: UUID,
    val channelId: UUID,
    val rating: Int,
    val comment: String?,
    val createdAt: Instant,
) {
    init {
        require(rating in 1..5) { "Rating must be between 1 and 5" }
    }
}
