package com.counseling.api.application

import com.counseling.api.domain.ChatMessage
import com.counseling.api.port.inbound.ChatHistoryResult
import com.counseling.api.port.inbound.ChatQuery
import com.counseling.api.port.outbound.ChatMessageReadRepository
import com.counseling.api.port.outbound.ChatNotificationPort
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Service
@Profile("!test")
class ChatQueryService(
    private val chatMessageReadRepository: ChatMessageReadRepository,
    private val chatNotificationPort: ChatNotificationPort,
) : ChatQuery {
    override fun getMessageHistory(
        channelId: UUID,
        before: Instant?,
        limit: Int,
    ): Mono<ChatHistoryResult> =
        chatMessageReadRepository
            .findByChannelId(channelId, before, limit + 1)
            .collectList()
            .map { messages ->
                val hasMore = messages.size > limit
                val trimmed = if (hasMore) messages.dropLast(1) else messages
                val reversed = trimmed.reversed()
                ChatHistoryResult(
                    messages = reversed,
                    hasMore = hasMore,
                    oldestTimestamp = reversed.firstOrNull()?.createdAt,
                )
            }

    override fun streamMessages(channelId: UUID): Flux<ChatMessage> = chatNotificationPort.subscribeMessages(channelId)
}
