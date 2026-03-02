package com.counseling.api.port.outbound

import com.counseling.api.domain.ChatMessage
import reactor.core.publisher.Flux
import java.util.UUID

interface ChatNotificationPort {
    fun emitMessage(
        channelId: UUID,
        message: ChatMessage,
    )

    fun subscribeMessages(channelId: UUID): Flux<ChatMessage>
}
