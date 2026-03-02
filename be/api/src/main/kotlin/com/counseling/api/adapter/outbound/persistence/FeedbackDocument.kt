package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.Feedback
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.util.UUID

@Document(collection = "feedbacks")
@CompoundIndexes(
    CompoundIndex(
        name = "idx_tenant_channel",
        def = "{'tenantId': 1, 'channelId': 1}",
        unique = true,
    ),
    CompoundIndex(
        name = "idx_tenant_created",
        def = "{'tenantId': 1, 'createdAt': -1}",
    ),
)
data class FeedbackDocument(
    @Id
    val id: String,
    val tenantId: String,
    val channelId: String,
    val rating: Int,
    val comment: String?,
    val createdAt: Instant,
) {
    fun toDomain(): Feedback =
        Feedback(
            id = UUID.fromString(id),
            channelId = UUID.fromString(channelId),
            rating = rating,
            comment = comment,
            createdAt = createdAt,
        )

    companion object {
        fun fromDomain(
            feedback: Feedback,
            tenantId: String,
        ): FeedbackDocument =
            FeedbackDocument(
                id = feedback.id.toString(),
                tenantId = tenantId,
                channelId = feedback.channelId.toString(),
                rating = feedback.rating,
                comment = feedback.comment,
                createdAt = feedback.createdAt,
            )
    }
}
