package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.Notification
import com.counseling.api.port.outbound.NotificationSsePort
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
@Profile("!test")
class NotificationSseAdapter : NotificationSsePort {
    private val log = LoggerFactory.getLogger(javaClass)
    private val recipientSinks = ConcurrentHashMap<UUID, Sinks.Many<Notification>>()

    override fun emit(
        recipientId: UUID,
        notification: Notification,
    ) {
        val result = recipientSink(recipientId).tryEmitNext(notification)
        if (result.isFailure) {
            log.warn("Failed to emit notification for recipient {}: {}", recipientId, result)
        }
    }

    override fun subscribe(recipientId: UUID): Flux<Notification> = recipientSink(recipientId).asFlux()

    override fun removeRecipient(recipientId: UUID) {
        recipientSinks.remove(recipientId)?.tryEmitComplete()
    }

    private fun recipientSink(recipientId: UUID): Sinks.Many<Notification> =
        recipientSinks.computeIfAbsent(recipientId) {
            Sinks.many().multicast().onBackpressureBuffer()
        }
}
