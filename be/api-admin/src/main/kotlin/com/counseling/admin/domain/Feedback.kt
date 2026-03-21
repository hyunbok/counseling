package com.counseling.admin.domain

import java.time.Instant

data class Feedback(
    val id: String? = null,
    val channelId: String,
    val rating: Int,
    val comment: String?,
    val createdAt: Instant,
) {
    init {
        require(rating in 1..5) { "Rating must be between 1 and 5" }
    }
}
