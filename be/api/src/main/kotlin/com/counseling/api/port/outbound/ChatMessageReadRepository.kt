package com.counseling.api.port.outbound

import com.counseling.api.domain.ChatMessage
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

interface ChatMessageReadRepository {
    fun save(message: ChatMessage): Mono<ChatMessage>

    fun findByChannelId(
        channelId: String,
        before: Instant?,
        limit: Int,
    ): Flux<ChatMessage>
}
