package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.ChatMessage
import com.counseling.api.domain.SenderType
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.util.UUID

@Document(collection = "chat_messages")
@CompoundIndex(name = "idx_channel_created", def = "{'channelId': 1, 'createdAt': -1}")
data class ChatMessageDocument(
    @Id
    val id: String,
    val channelId: String,
    val senderType: String,
    val senderId: String,
    val content: String,
    val createdAt: Instant,
) {
    fun toDomain(): ChatMessage =
        ChatMessage(
            id = UUID.fromString(id),
            channelId = UUID.fromString(channelId),
            senderType = SenderType.valueOf(senderType),
            senderId = senderId,
            content = content,
            createdAt = createdAt,
        )

    companion object {
        fun fromDomain(message: ChatMessage): ChatMessageDocument =
            ChatMessageDocument(
                id = message.id.toString(),
                channelId = message.channelId.toString(),
                senderType = message.senderType.name,
                senderId = message.senderId,
                content = message.content,
                createdAt = message.createdAt,
            )
    }
}
