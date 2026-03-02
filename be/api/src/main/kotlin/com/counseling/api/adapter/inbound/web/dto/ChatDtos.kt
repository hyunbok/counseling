package com.counseling.api.adapter.inbound.web.dto

import com.counseling.api.domain.SenderType
import java.time.Instant
import java.util.UUID

data class SendChatMessageRequest(
    val senderType: SenderType,
    val senderId: String,
    val content: String,
)

data class ChatMessageResponse(
    val id: UUID,
    val channelId: UUID,
    val senderType: String,
    val senderId: String,
    val content: String,
    val createdAt: Instant,
)

data class ChatMessageListResponse(
    val messages: List<ChatMessageResponse>,
    val hasMore: Boolean,
    val oldestTimestamp: Instant?,
)
