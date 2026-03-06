package com.counseling.admin.adapter.outbound.persistence

import com.counseling.admin.application.TenantContext
import com.counseling.admin.port.inbound.AgentStats
import com.counseling.admin.port.inbound.StatsSummary
import com.counseling.admin.port.outbound.AdminStatsRepository
import org.bson.Document
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Repository
@Primary
@Profile("!test")
class AdminStatsMongoRepository(
    private val mongoTemplate: ReactiveMongoTemplate,
) : AdminStatsRepository {
    override fun getSummary(
        from: Instant,
        to: Instant,
    ): Mono<StatsSummary> =
        TenantContext.getTenantId().flatMap { tenantId ->
            val matchCriteria =
                Criteria
                    .where("tenantId")
                    .`is`(tenantId)
                    .and("startedAt")
                    .gte(from)
                    .lt(to)

            val aggregation =
                Aggregation.newAggregation(
                    Aggregation.match(matchCriteria),
                    Aggregation
                        .group()
                        .count()
                        .`as`("totalChannels")
                        .sum(
                            org.springframework.data.mongodb.core.aggregation.ConditionalOperators
                                .`when`(Criteria.where("status").`is`("CLOSED"))
                                .then(1)
                                .otherwise(0),
                        ).`as`("completedChannels")
                        .avg("feedback.rating")
                        .`as`("avgRating")
                        .avg("durationSeconds")
                        .`as`("avgHandleTime"),
                )

            mongoTemplate
                .aggregate(aggregation, COLLECTION_NAME, Document::class.java)
                .next()
                .map { doc ->
                    StatsSummary(
                        totalChannels = toLong(doc.get("totalChannels")),
                        completedChannels = toLong(doc.get("completedChannels")),
                        averageRating = toDouble(doc.get("avgRating")),
                        averageHandleTimeSeconds = toLong(doc.get("avgHandleTime")),
                    )
                }.defaultIfEmpty(StatsSummary(0L, 0L, 0.0, 0L))
        }

    override fun getAgentStats(
        from: Instant,
        to: Instant,
    ): Mono<List<AgentStats>> =
        TenantContext.getTenantId().flatMap { tenantId ->
            val matchCriteria =
                Criteria
                    .where("tenantId")
                    .`is`(tenantId)
                    .and("startedAt")
                    .gte(from)
                    .lt(to)
                    .and("agentId")
                    .exists(true)
                    .ne(null)

            val aggregation =
                Aggregation.newAggregation(
                    Aggregation.match(matchCriteria),
                    Aggregation
                        .group("agentId", "agentName")
                        .count()
                        .`as`("totalChannels")
                        .sum(
                            org.springframework.data.mongodb.core.aggregation.ConditionalOperators
                                .`when`(Criteria.where("status").`is`("CLOSED"))
                                .then(1)
                                .otherwise(0),
                        ).`as`("completedChannels")
                        .avg("feedback.rating")
                        .`as`("avgRating")
                        .avg("durationSeconds")
                        .`as`("avgHandleTime"),
                    Aggregation.sort(
                        org.springframework.data.domain.Sort.by(
                            org.springframework.data.domain.Sort.Direction.DESC,
                            "totalChannels",
                        ),
                    ),
                )

            mongoTemplate
                .aggregate(aggregation, COLLECTION_NAME, Document::class.java)
                .map { doc ->
                    val idDoc = doc.get("_id", Document::class.java)
                    AgentStats(
                        agentId = UUID.fromString(idDoc?.getString("agentId") ?: ""),
                        agentName = idDoc?.getString("agentName") ?: "",
                        totalChannels = toLong(doc.get("totalChannels")),
                        completedChannels = toLong(doc.get("completedChannels")),
                        averageRating = toDouble(doc.get("avgRating")),
                        averageHandleTimeSeconds = toLong(doc.get("avgHandleTime")),
                    )
                }.collectList()
        }

    private fun toLong(value: Any?): Long =
        when (value) {
            is Long -> value
            is Int -> value.toLong()
            is Double -> value.toLong()
            is Number -> value.toLong()
            else -> 0L
        }

    private fun toDouble(value: Any?): Double =
        when (value) {
            is Double -> value
            is Float -> value.toDouble()
            is Number -> value.toDouble()
            else -> 0.0
        }

    companion object {
        private const val COLLECTION_NAME = "channel_histories"
    }
}
