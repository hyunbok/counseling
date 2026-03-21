package com.counseling.api.adapter.outbound.external

import com.counseling.api.domain.CoBrowsingSession
import com.counseling.api.port.outbound.CoBrowsingNotificationPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap

@Component
class InMemoryCoBrowsingNotificationAdapter : CoBrowsingNotificationPort {
    private val log = LoggerFactory.getLogger(javaClass)
    private val channelSinks = ConcurrentHashMap<String, Sinks.Many<CoBrowsingSession>>()

    override fun emitSessionUpdate(
        channelId: String,
        session: CoBrowsingSession,
    ) {
        val result = channelSink(channelId).tryEmitNext(session)
        if (result.isFailure) {
            log.warn("Failed to emit co-browsing session update for channel {}: {}", channelId, result)
        }
    }

    override fun subscribeSessionUpdates(channelId: String): Flux<CoBrowsingSession> = channelSink(channelId).asFlux()

    override fun removeChannel(channelId: String) {
        channelSinks.remove(channelId)?.tryEmitComplete()
    }

    private fun channelSink(channelId: String): Sinks.Many<CoBrowsingSession> =
        channelSinks.computeIfAbsent(channelId) {
            Sinks.many().multicast().onBackpressureBuffer()
        }
}
