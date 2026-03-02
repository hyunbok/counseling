package com.counseling.api.port.outbound

import com.counseling.api.domain.ChatMessage
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

interface ChatMessageReadRepository {
    fun save(message: ChatMessage): Mono<ChatMessage>

    fun findByChannelId(
        channelId: UUID,
        before: Instant?,
        limit: Int,
    ): Flux<ChatMessage>
}
