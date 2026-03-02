package com.counseling.api.port.inbound

import com.counseling.api.domain.ChatMessage
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

data class ChatHistoryResult(
    val messages: List<ChatMessage>,
    val hasMore: Boolean,
    val oldestTimestamp: Instant?,
)

interface ChatQuery {
    fun getMessageHistory(
        channelId: UUID,
        before: Instant?,
        limit: Int,
    ): Mono<ChatHistoryResult>

    fun streamMessages(channelId: UUID): Flux<ChatMessage>
}
