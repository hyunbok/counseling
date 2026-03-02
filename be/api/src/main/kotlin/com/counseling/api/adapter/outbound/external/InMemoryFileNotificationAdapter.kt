package com.counseling.api.adapter.outbound.external

import com.counseling.api.domain.SharedFile
import com.counseling.api.port.outbound.FileNotificationPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class InMemoryFileNotificationAdapter : FileNotificationPort {
    private val log = LoggerFactory.getLogger(javaClass)
    private val channelSinks = ConcurrentHashMap<UUID, Sinks.Many<SharedFile>>()

    override fun emitFile(
        channelId: UUID,
        file: SharedFile,
    ) {
        val result = channelSink(channelId).tryEmitNext(file)
        if (result.isFailure) {
            log.warn("Failed to emit file event for channel {}: {}", channelId, result)
        }
    }

    override fun subscribeFiles(channelId: UUID): Flux<SharedFile> = channelSink(channelId).asFlux()

    override fun removeChannel(channelId: UUID) {
        channelSinks.remove(channelId)?.tryEmitComplete()
    }

    private fun channelSink(channelId: UUID): Sinks.Many<SharedFile> =
        channelSinks.computeIfAbsent(channelId) {
            Sinks.many().multicast().onBackpressureBuffer()
        }
}
