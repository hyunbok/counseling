package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.Feedback
import com.counseling.api.port.outbound.FeedbackRepository
import io.r2dbc.spi.Readable
import org.springframework.context.annotation.Profile
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Repository
@Profile("!test")
class FeedbackR2dbcRepository(
    private val databaseClient: DatabaseClient,
) : FeedbackRepository {
    override fun save(feedback: Feedback): Mono<Feedback> {
        val spec =
            databaseClient
                .sql(
                    """
                    INSERT INTO feedbacks (id, channel_id, rating, comment, created_at)
                    VALUES (:id, :channelId, :rating, :comment, :createdAt)
                    ON CONFLICT (channel_id) DO NOTHING
                    """.trimIndent(),
                ).bind("id", feedback.id)
                .bind("channelId", feedback.channelId)
                .bind("rating", feedback.rating)
                .bind("createdAt", feedback.createdAt)
        val specWithComment =
            if (feedback.comment != null) {
                spec.bind("comment", feedback.comment)
            } else {
                spec.bindNull("comment", String::class.java)
            }
        return specWithComment.then().thenReturn(feedback)
    }

    override fun findByChannelId(channelId: UUID): Mono<Feedback> =
        databaseClient
            .sql("SELECT * FROM feedbacks WHERE channel_id = :channelId")
            .bind("channelId", channelId)
            .map { row -> mapToFeedback(row) }
            .one()

    private fun mapToFeedback(row: Readable): Feedback =
        Feedback(
            id = row.get("id", UUID::class.java)!!,
            channelId = row.get("channel_id", UUID::class.java)!!,
            rating = row.get("rating", Integer::class.java)!!.toInt(),
            comment = row.get("comment", String::class.java),
            createdAt = row.get("created_at", Instant::class.java)!!,
        )
}
