package com.counseling.api.domain

import java.time.Instant

data class ChatMessage(
    val id: String? = null,
    val channelId: String,
    val senderType: SenderType,
    val senderId: String,
    val content: String,
    val createdAt: Instant,
)
