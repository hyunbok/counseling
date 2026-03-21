package com.counseling.api.port.inbound

import com.counseling.api.domain.ChatMessage
import com.counseling.api.domain.SenderType
import reactor.core.publisher.Mono

data class SendMessageCommand(
    val channelId: String,
    val senderType: SenderType,
    val senderId: String,
    val content: String,
)

interface ChatUseCase {
    fun sendMessage(command: SendMessageCommand): Mono<ChatMessage>
}
