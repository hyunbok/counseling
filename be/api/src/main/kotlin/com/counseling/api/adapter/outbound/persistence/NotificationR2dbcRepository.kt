package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.DeliveryMethod
import com.counseling.api.domain.Notification
import com.counseling.api.domain.NotificationType
import com.counseling.api.domain.RecipientType
import com.counseling.api.port.outbound.NotificationRepository
import io.r2dbc.spi.Readable
import org.springframework.context.annotation.Profile
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Repository
@Profile("!test")
class NotificationR2dbcRepository(
    private val databaseClient: DatabaseClient,
) : NotificationRepository {
    override fun save(notification: Notification): Mono<Notification> {
        val spec =
            databaseClient
                .sql(
                    """
                    INSERT INTO notifications (id, recipient_id, recipient_type, type, title, body, reference_id, reference_type, delivery_method, read, created_at)
                    VALUES (:id, :recipientId, :recipientType, :type, :title, :body, :referenceId, :referenceType, :deliveryMethod, :read, :createdAt)
                    RETURNING *
                    """.trimIndent(),
                ).bind("id", notification.id)
                .bind("recipientId", notification.recipientId)
                .bind("recipientType", notification.recipientType.name)
                .bind("type", notification.type.name)
                .bind("title", notification.title)
                .bind("body", notification.body)
                .bind("deliveryMethod", notification.deliveryMethod.name)
                .bind("read", notification.read)
                .bind("createdAt", notification.createdAt)
        val specWithReferenceId =
            if (notification.referenceId != null) {
                spec.bind("referenceId", notification.referenceId)
            } else {
                spec.bindNull("referenceId", UUID::class.java)
            }
        val specWithReferenceType =
            if (notification.referenceType != null) {
                specWithReferenceId.bind("referenceType", notification.referenceType)
            } else {
                specWithReferenceId.bindNull("referenceType", String::class.java)
            }
        return specWithReferenceType
            .map { row: Readable -> mapToNotification(row) }
            .one()
    }

    override fun findByIdAndRecipientId(
        id: UUID,
        recipientId: UUID,
    ): Mono<Notification> =
        databaseClient
            .sql(
                """
                SELECT id, recipient_id, recipient_type, type, title, body, reference_id, reference_type, delivery_method, read, created_at
                FROM notifications
                WHERE id = :id AND recipient_id = :recipientId AND deleted = FALSE
                """.trimIndent(),
            ).bind("id", id)
            .bind("recipientId", recipientId)
            .map { row: Readable -> mapToNotification(row) }
            .one()

    override fun markAsRead(
        id: UUID,
        recipientId: UUID,
    ): Mono<Boolean> =
        databaseClient
            .sql(
                """
                UPDATE notifications SET read = TRUE WHERE id = :id AND recipient_id = :recipientId AND deleted = FALSE
                """.trimIndent(),
            ).bind("id", id)
            .bind("recipientId", recipientId)
            .fetch()
            .rowsUpdated()
            .map { it > 0 }

    override fun markAllAsReadByRecipientId(recipientId: UUID): Mono<Long> =
        databaseClient
            .sql(
                """
                UPDATE notifications SET read = TRUE WHERE recipient_id = :recipientId AND read = FALSE AND deleted = FALSE
                """.trimIndent(),
            ).bind("recipientId", recipientId)
            .fetch()
            .rowsUpdated()

    private fun mapToNotification(row: Readable): Notification =
        Notification(
            id = row.get("id", UUID::class.java)!!,
            recipientId = row.get("recipient_id", UUID::class.java)!!,
            recipientType = RecipientType.valueOf(row.get("recipient_type", String::class.java)!!),
            type = NotificationType.valueOf(row.get("type", String::class.java)!!),
            title = row.get("title", String::class.java)!!,
            body = row.get("body", String::class.java)!!,
            referenceId = row.get("reference_id", UUID::class.java),
            referenceType = row.get("reference_type", String::class.java),
            deliveryMethod = DeliveryMethod.valueOf(row.get("delivery_method", String::class.java)!!),
            read = row.get("read", Boolean::class.java)!!,
            createdAt = row.get("created_at", Instant::class.java)!!,
        )
}
