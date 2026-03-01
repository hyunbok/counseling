package com.counseling.api.domain

import java.time.Instant
import java.util.UUID

data class ChatMessage(
    val id: UUID,
    val channelId: UUID,
    val senderType: SenderType,
    val senderId: String,
    val content: String,
    val createdAt: Instant,
)
