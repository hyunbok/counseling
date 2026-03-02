package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.Feedback
import com.counseling.api.domain.FeedbackStatsResult
import com.counseling.api.port.outbound.FeedbackReadRepository
import org.bson.Document
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Repository
@Profile("!test")
class FeedbackMongoRepository(
    private val mongoTemplate: ReactiveMongoTemplate,
) : FeedbackReadRepository {
    override fun save(
        feedback: Feedback,
        tenantId: String,
    ): Mono<Feedback> =
        mongoTemplate
            .save(FeedbackDocument.fromDomain(feedback, tenantId), COLLECTION_NAME)
            .map { it.toDomain() }

    override fun findByChannelId(
        channelId: UUID,
        tenantId: String,
    ): Mono<Feedback> {
        val criteria =
            Criteria
                .where("tenantId")
                .`is`(tenantId)
                .and("channelId")
                .`is`(channelId.toString())
        return mongoTemplate
            .findOne(Query.query(criteria), FeedbackDocument::class.java, COLLECTION_NAME)
            .map { it.toDomain() }
    }

    override fun getStats(
        tenantId: String,
        from: Instant,
        to: Instant,
    ): Mono<FeedbackStatsResult> {
        val matchCriteria =
            Criteria
                .where("tenantId")
                .`is`(tenantId)
                .and("createdAt")
                .gte(from)
                .lte(to)

        val aggregation =
            Aggregation.newAggregation(
                Aggregation.match(matchCriteria),
                Aggregation
                    .facet(
                        Aggregation
                            .group()
                            .count()
                            .`as`("totalCount")
                            .avg("rating")
                            .`as`("averageRating"),
                    ).`as`("summary")
                    .and(
                        Aggregation.group("rating").count().`as`("count"),
                    ).`as`("distribution"),
            )

        return mongoTemplate
            .aggregate(aggregation, COLLECTION_NAME, Document::class.java)
            .next()
            .map { doc ->
                @Suppress("UNCHECKED_CAST")
                val summaryList = doc.getList("summary", Document::class.java) ?: emptyList()
                val summary = summaryList.firstOrNull()

                @Suppress("UNCHECKED_CAST")
                val distList = doc.getList("distribution", Document::class.java) ?: emptyList()
                val distribution =
                    (1..5).associateWith { r ->
                        distList
                            .firstOrNull { it.getInteger("_id") == r }
                            ?.getInteger("count")
                            ?.toLong() ?: 0L
                    }

                FeedbackStatsResult(
                    totalCount = summary?.getInteger("totalCount", 0)?.toLong() ?: 0L,
                    averageRating = summary?.getDouble("averageRating") ?: 0.0,
                    distribution = distribution,
                    from = from,
                    to = to,
                )
            }.defaultIfEmpty(
                FeedbackStatsResult(
                    totalCount = 0,
                    averageRating = 0.0,
                    distribution = (1..5).associateWith { 0L },
                    from = from,
                    to = to,
                ),
            )
    }

    companion object {
        private const val COLLECTION_NAME = "feedbacks"
    }
}
