package com.counseling.api.adapter.outbound.external

import com.counseling.api.domain.ChatMessage
import com.counseling.api.port.outbound.ChatNotificationPort
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class InMemoryChatNotificationAdapter : ChatNotificationPort {

    private val channelSinks = ConcurrentHashMap<UUID, Sinks.Many<ChatMessage>>()

    override fun emitMessage(
        channelId: UUID,
        message: ChatMessage,
    ) {
        channelSink(channelId).tryEmitNext(message)
    }

    override fun subscribeMessages(channelId: UUID): Flux<ChatMessage> =
        channelSink(channelId).asFlux()

    private fun channelSink(channelId: UUID): Sinks.Many<ChatMessage> =
        channelSinks.computeIfAbsent(channelId) {
            Sinks.many().multicast().onBackpressureBuffer()
        }
}
