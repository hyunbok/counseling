package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.ChatMessage
import com.counseling.api.domain.SenderType
import com.counseling.api.port.outbound.ChatMessageRepository
import io.r2dbc.spi.Readable
import org.springframework.context.annotation.Profile
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Repository
@Profile("!test")
class ChatMessageR2dbcRepository(
    private val databaseClient: DatabaseClient,
) : ChatMessageRepository {
    override fun save(message: ChatMessage): Mono<ChatMessage> =
        databaseClient
            .sql(
                """
                INSERT INTO chat_messages (id, channel_id, sender_type, sender_id, content, created_at)
                VALUES (:id, :channelId, :senderType, :senderId, :content, :createdAt)
                """.trimIndent(),
            ).bind("id", message.id)
            .bind("channelId", message.channelId)
            .bind("senderType", message.senderType.name)
            .bind("senderId", message.senderId)
            .bind("content", message.content)
            .bind("createdAt", message.createdAt)
            .then()
            .thenReturn(message)

    override fun findAllByChannelId(channelId: UUID): Flux<ChatMessage> =
        databaseClient
            .sql("SELECT * FROM chat_messages WHERE channel_id = :channelId ORDER BY created_at")
            .bind("channelId", channelId)
            .map { row -> mapToChatMessage(row) }
            .all()

    override fun findByChannelIdBefore(
        channelId: UUID,
        before: Instant,
        limit: Int,
    ): Flux<ChatMessage> =
        databaseClient
            .sql(
                """
                SELECT id, channel_id, sender_type, sender_id, content, created_at
                FROM chat_messages
                WHERE channel_id = :channelId AND created_at < :before
                ORDER BY created_at DESC
                LIMIT :limit
                """.trimIndent(),
            ).bind("channelId", channelId)
            .bind("before", before)
            .bind("limit", limit)
            .map { row -> mapToChatMessage(row) }
            .all()

    private fun mapToChatMessage(row: Readable): ChatMessage =
        ChatMessage(
            id = row.get("id", UUID::class.java)!!,
            channelId = row.get("channel_id", UUID::class.java)!!,
            senderType = SenderType.valueOf(row.get("sender_type", String::class.java)!!),
            senderId = row.get("sender_id", String::class.java)!!,
            content = row.get("content", String::class.java)!!,
            createdAt = row.get("created_at", Instant::class.java)!!,
        )
}
