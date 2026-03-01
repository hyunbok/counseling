package com.counseling.api.port.outbound

import com.counseling.api.domain.ChatMessage
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface ChatMessageRepository {
    fun save(message: ChatMessage): Mono<ChatMessage>

    fun findAllByChannelId(channelId: UUID): Flux<ChatMessage>
}
