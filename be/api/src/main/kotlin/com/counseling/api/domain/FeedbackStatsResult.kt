package com.counseling.api.domain

import java.time.Instant

data class FeedbackStatsResult(
    val totalCount: Long,
    val averageRating: Double,
    val distribution: Map<Int, Long>,
    val from: Instant,
    val to: Instant,
)
