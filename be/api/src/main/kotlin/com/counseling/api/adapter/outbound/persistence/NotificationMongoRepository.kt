package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.Notification
import com.counseling.api.domain.NotificationType
import com.counseling.api.port.outbound.NotificationReadRepository
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
class NotificationMongoRepository(
    private val mongoTemplate: ReactiveMongoTemplate,
) : NotificationReadRepository {
    override fun save(
        notification: Notification,
        tenantId: String,
    ): Mono<Notification> =
        mongoTemplate
            .save(NotificationDocument.fromDomain(notification, tenantId), COLLECTION_NAME)
            .map { it.toDomain() }

    override fun findByRecipientId(
        recipientId: UUID,
        tenantId: String,
        type: NotificationType?,
        read: Boolean?,
        before: Instant?,
        limit: Int,
    ): Flux<Notification> {
        var criteria =
            Criteria
                .where("tenantId")
                .`is`(tenantId)
                .and("recipientId")
                .`is`(recipientId.toString())
        if (type != null) {
            criteria = criteria.and("type").`is`(type.name)
        }
        if (read != null) {
            criteria = criteria.and("read").`is`(read)
        }
        if (before != null) {
            criteria = criteria.and("createdAt").lt(before)
        }
        val query =
            Query
                .query(criteria)
                .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                .limit(limit)
        return mongoTemplate
            .find(query, NotificationDocument::class.java, COLLECTION_NAME)
            .map { it.toDomain() }
    }

    override fun countUnread(
        recipientId: UUID,
        tenantId: String,
    ): Mono<Long> {
        val criteria =
            Criteria
                .where("tenantId")
                .`is`(tenantId)
                .and("recipientId")
                .`is`(recipientId.toString())
                .and("read")
                .`is`(false)
        return mongoTemplate.count(Query.query(criteria), NotificationDocument::class.java, COLLECTION_NAME)
    }

    override fun markAsRead(
        notificationId: UUID,
        tenantId: String,
    ): Mono<Boolean> {
        val criteria =
            Criteria
                .where("tenantId")
                .`is`(tenantId)
                .and("_id")
                .`is`(notificationId.toString())
        return mongoTemplate
            .updateFirst(
                Query.query(criteria),
                Update.update("read", true),
                NotificationDocument::class.java,
                COLLECTION_NAME,
            ).map { it.modifiedCount > 0 }
    }

    override fun markAllAsRead(
        recipientId: UUID,
        tenantId: String,
    ): Mono<Long> {
        val criteria =
            Criteria
                .where("tenantId")
                .`is`(tenantId)
                .and("recipientId")
                .`is`(recipientId.toString())
                .and("read")
                .`is`(false)
        return mongoTemplate
            .updateMulti(
                Query.query(criteria),
                Update.update("read", true),
                NotificationDocument::class.java,
                COLLECTION_NAME,
            ).map { it.modifiedCount }
    }

    companion object {
        private const val COLLECTION_NAME = "notifications"
    }
}
