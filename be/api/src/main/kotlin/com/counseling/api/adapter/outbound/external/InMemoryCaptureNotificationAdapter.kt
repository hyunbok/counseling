package com.counseling.api.adapter.outbound.external

import com.counseling.api.domain.ScreenCapture
import com.counseling.api.port.outbound.CaptureNotificationPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class InMemoryCaptureNotificationAdapter : CaptureNotificationPort {
    private val log = LoggerFactory.getLogger(javaClass)
    private val channelSinks = ConcurrentHashMap<UUID, Sinks.Many<ScreenCapture>>()

    override fun emitCapture(
        channelId: UUID,
        capture: ScreenCapture,
    ) {
        val result = channelSink(channelId).tryEmitNext(capture)
        if (result.isFailure) {
            log.warn("Failed to emit capture event for channel {}: {}", channelId, result)
        }
    }

    override fun subscribeCaptures(channelId: UUID): Flux<ScreenCapture> = channelSink(channelId).asFlux()

    override fun removeChannel(channelId: UUID) {
        channelSinks.remove(channelId)?.tryEmitComplete()
    }

    private fun channelSink(channelId: UUID): Sinks.Many<ScreenCapture> =
        channelSinks.computeIfAbsent(channelId) {
            Sinks.many().multicast().onBackpressureBuffer()
        }
}
