package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.ChatMessage
import com.counseling.api.domain.SenderType
import java.time.Instant
import java.util.UUID

data class ChatMessageDocument(
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
