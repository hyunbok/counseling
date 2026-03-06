package com.counseling.admin.adapter.outbound.persistence

import com.counseling.admin.application.TenantContext
import com.counseling.admin.domain.Feedback
import com.counseling.admin.port.outbound.AdminFeedbackRepository
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Repository
@Primary
@Profile("!test")
class AdminFeedbackMongoRepository(
    private val mongoTemplate: ReactiveMongoTemplate,
) : AdminFeedbackRepository {
    override fun findById(id: UUID): Mono<Feedback> =
        TenantContext.getTenantId().flatMap { tenantId ->
            val criteria =
                Criteria
                    .where("_id")
                    .`is`(id.toString())
                    .and("tenantId")
                    .`is`(tenantId)
            mongoTemplate
                .findOne(
                    Query.query(criteria),
                    FeedbackReadDocument::class.java,
                    COLLECTION_NAME,
                ).map { it.toDomain() }
        }

    override fun findAll(
        agentId: UUID?,
        rating: Int?,
        page: Int,
        size: Int,
    ): Flux<Feedback> =
        TenantContext.getTenantId().flatMapMany { tenantId ->
            if (agentId != null) {
                findAllWithAgentFilter(tenantId, agentId, rating, page, size)
            } else {
                findAllDirect(tenantId, rating, page, size)
            }
        }

    override fun countAll(
        agentId: UUID?,
        rating: Int?,
    ): Mono<Long> =
        TenantContext.getTenantId().flatMap { tenantId ->
            if (agentId != null) {
                countWithAgentFilter(tenantId, agentId, rating)
            } else {
                countDirect(tenantId, rating)
            }
        }

    private fun findAllDirect(
        tenantId: String,
        rating: Int?,
        page: Int,
        size: Int,
    ): Flux<Feedback> {
        var criteria = Criteria.where("tenantId").`is`(tenantId)
        if (rating != null) {
            criteria = criteria.and("rating").`is`(rating)
        }
        val query =
            Query
                .query(criteria)
                .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                .skip((page * size).toLong())
                .limit(size)
        return mongoTemplate
            .find(query, FeedbackReadDocument::class.java, COLLECTION_NAME)
            .map { it.toDomain() }
    }

    private fun countDirect(
        tenantId: String,
        rating: Int?,
    ): Mono<Long> {
        var criteria = Criteria.where("tenantId").`is`(tenantId)
        if (rating != null) {
            criteria = criteria.and("rating").`is`(rating)
        }
        return mongoTemplate.count(Query.query(criteria), COLLECTION_NAME)
    }

    private fun findAllWithAgentFilter(
        tenantId: String,
        agentId: UUID,
        rating: Int?,
        page: Int,
        size: Int,
    ): Flux<Feedback> {
        val historyCriteria =
            Criteria
                .where("tenantId")
                .`is`(tenantId)
                .and("agentId")
                .`is`(agentId.toString())
                .and("feedback")
                .exists(true)
        val historyQuery =
            Query
                .query(historyCriteria)
                .with(Sort.by(Sort.Direction.DESC, "endedAt"))
        historyQuery.fields().include("channelId")
        return mongoTemplate
            .find(historyQuery, ChannelIdOnly::class.java, HISTORY_COLLECTION)
            .map { UUID.fromString(it.channelId) }
            .collectList()
            .flatMapMany { channelIds ->
                if (channelIds.isEmpty()) {
                    return@flatMapMany Flux.empty()
                }
                var criteria =
                    Criteria
                        .where("tenantId")
                        .`is`(tenantId)
                        .and("channelId")
                        .`in`(channelIds.map { it.toString() })
                if (rating != null) {
                    criteria = criteria.and("rating").`is`(rating)
                }
                val query =
                    Query
                        .query(criteria)
                        .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                        .skip((page * size).toLong())
                        .limit(size)
                mongoTemplate
                    .find(query, FeedbackReadDocument::class.java, COLLECTION_NAME)
                    .map { it.toDomain() }
            }
    }

    private fun countWithAgentFilter(
        tenantId: String,
        agentId: UUID,
        rating: Int?,
    ): Mono<Long> {
        val historyCriteria =
            Criteria
                .where("tenantId")
                .`is`(tenantId)
                .and("agentId")
                .`is`(agentId.toString())
                .and("feedback")
                .exists(true)
        val historyQuery = Query.query(historyCriteria)
        historyQuery.fields().include("channelId")
        return mongoTemplate
            .find(historyQuery, ChannelIdOnly::class.java, HISTORY_COLLECTION)
            .map { it.channelId }
            .collectList()
            .flatMap { channelIds ->
                if (channelIds.isEmpty()) {
                    return@flatMap Mono.just(0L)
                }
                var criteria =
                    Criteria
                        .where("tenantId")
                        .`is`(tenantId)
                        .and("channelId")
                        .`in`(channelIds)
                if (rating != null) {
                    criteria = criteria.and("rating").`is`(rating)
                }
                mongoTemplate.count(Query.query(criteria), COLLECTION_NAME)
            }
    }

    companion object {
        private const val COLLECTION_NAME = "feedbacks"
        private const val HISTORY_COLLECTION = "channel_histories"
    }
}

private data class FeedbackReadDocument(
    val id: String? = null,
    val tenantId: String? = null,
    val channelId: String? = null,
    val rating: Int = 0,
    val comment: String? = null,
    val createdAt: Instant? = null,
) {
    fun toDomain(): Feedback =
        Feedback(
            id = UUID.fromString(id),
            channelId = UUID.fromString(channelId),
            rating = rating,
            comment = comment,
            createdAt = createdAt ?: Instant.now(),
        )
}

private data class ChannelIdOnly(
    val channelId: String = "",
)
