package com.counseling.admin.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("feedbacks")
data class Feedback(
    @Id val id: String? = null,
    @Column("channel_id") val channelId: String,
    val rating: Int,
    val comment: String?,
    @Column("created_at") val createdAt: Instant,
) {
    init {
        require(rating in 1..5) { "Rating must be between 1 and 5" }
    }
}
