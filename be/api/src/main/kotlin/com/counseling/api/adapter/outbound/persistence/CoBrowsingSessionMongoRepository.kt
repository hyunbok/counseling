package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.CoBrowsingSession
import com.counseling.api.domain.CoBrowsingStatus
import com.counseling.api.port.outbound.CoBrowsingSessionReadRepository
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
class CoBrowsingSessionMongoRepository(
    private val mongoTemplate: ReactiveMongoTemplate,
) : CoBrowsingSessionReadRepository {
    override fun save(session: CoBrowsingSession): Mono<CoBrowsingSession> =
        mongoTemplate
            .save(CoBrowsingSessionDocument.fromDomain(session), COLLECTION_NAME)
            .map { it.toDomain() }

    override fun findByChannelId(
        channelId: UUID,
        before: Instant?,
        limit: Int,
    ): Flux<CoBrowsingSession> {
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
            .find(query, CoBrowsingSessionDocument::class.java, COLLECTION_NAME)
            .map { it.toDomain() }
    }

    override fun updateStatus(
        id: UUID,
        status: CoBrowsingStatus,
        startedAt: Instant?,
        endedAt: Instant?,
        updatedAt: Instant,
    ): Mono<Void> {
        val query = Query.query(Criteria.where("_id").`is`(id.toString()))
        val update =
            Update
                .update("status", status.name)
                .set("updatedAt", updatedAt)
        if (startedAt != null) update.set("startedAt", startedAt)
        if (endedAt != null) update.set("endedAt", endedAt)
        return mongoTemplate
            .updateFirst(query, update, CoBrowsingSessionDocument::class.java, COLLECTION_NAME)
            .then()
    }

    companion object {
        private const val COLLECTION_NAME = "co_browsing_sessions"
    }
}
