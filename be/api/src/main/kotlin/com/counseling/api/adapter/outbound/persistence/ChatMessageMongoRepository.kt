package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.ChatMessage
import com.counseling.api.port.outbound.ChatMessageReadRepository
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
@Profile("!test")
class ChatMessageMongoRepository(
    private val mongoTemplate: ReactiveMongoTemplate,
) : ChatMessageReadRepository {
    override fun save(message: ChatMessage): Mono<ChatMessage> =
        mongoTemplate
            .save(ChatMessageDocument.fromDomain(message), COLLECTION_NAME)
            .map { it.toDomain() }

    override fun findByChannelId(
        channelId: UUID,
        before: Instant?,
        limit: Int,
    ): Flux<ChatMessage> {
        val criteria =
            if (before != null) {
                Criteria
                    .where("channelId")
                    .`is`(channelId.toString())
                    .and("createdAt")
                    .lt(before)
            } else {
                Criteria
                    .where("channelId")
                    .`is`(channelId.toString())
            }
        val query =
            Query
                .query(criteria)
                .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                .limit(limit)
        return mongoTemplate
            .find(query, ChatMessageDocument::class.java, COLLECTION_NAME)
            .map { it.toDomain() }
    }

    companion object {
        private const val COLLECTION_NAME = "chat_messages"
    }
}
