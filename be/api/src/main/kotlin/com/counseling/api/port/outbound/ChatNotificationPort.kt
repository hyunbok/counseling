package com.counseling.api.port.outbound

import com.counseling.api.domain.ChatMessage
import reactor.core.publisher.Flux

interface ChatNotificationPort {
    fun emitMessage(
        channelId: String,
        message: ChatMessage,
    )

    fun subscribeMessages(channelId: String): Flux<ChatMessage>

    fun removeChannel(channelId: String)
}
