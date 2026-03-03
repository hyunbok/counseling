package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.ScreenCapture
import com.counseling.api.port.outbound.ScreenCaptureReadRepository
import org.springframework.context.annotation.Profile
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Repository
@Profile("!test")
class ScreenCaptureMongoRepository(
    private val mongoTemplate: ReactiveMongoTemplate,
) : ScreenCaptureReadRepository {
    override fun save(capture: ScreenCapture): Mono<ScreenCapture> =
        mongoTemplate
            .save(ScreenCaptureDocument.fromDomain(capture), COLLECTION_NAME)
            .map { it.toDomain() }

    override fun findByChannelId(
        channelId: UUID,
        before: Instant?,
        limit: Int,
    ): Flux<ScreenCapture> {
        val criteria =
            if (before != null) {
                Criteria
                    .where("channelId")
                    .`is`(channelId.toString())
                    .and("deleted")
                    .`is`(false)
                    .and("createdAt")
                    .lt(before)
            } else {
                Criteria
                    .where("channelId")
                    .`is`(channelId.toString())
                    .and("deleted")
                    .`is`(false)
            }
        val query =
            Query
                .query(criteria)
                .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                .limit(limit)
        return mongoTemplate
            .find(query, ScreenCaptureDocument::class.java, COLLECTION_NAME)
            .map { it.toDomain() }
    }

    override fun markDeleted(id: UUID): Mono<Void> {
        val query = Query.query(Criteria.where("_id").`is`(id.toString()))
        val update = Update.update("deleted", true)
        return mongoTemplate
            .updateFirst(query, update, ScreenCaptureDocument::class.java, COLLECTION_NAME)
            .then()
    }

    companion object {
        private const val COLLECTION_NAME = "screen_captures"
    }
}
