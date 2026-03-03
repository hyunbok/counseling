package com.counseling.api.adapter.outbound.external

import com.counseling.api.domain.PositionUpdate
import com.counseling.api.domain.QueueUpdate
import com.counseling.api.port.outbound.QueueNotificationPort
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import reactor.util.concurrent.Queues
import java.util.concurrent.ConcurrentHashMap

@Component
class InMemoryQueueNotificationAdapter : QueueNotificationPort {
    private val agentSinks = ConcurrentHashMap<String, Sinks.Many<QueueUpdate>>()
    private val positionSinks = ConcurrentHashMap<String, Sinks.Many<PositionUpdate>>()

    private fun agentSink(tenantId: String): Sinks.Many<QueueUpdate> =
        agentSinks.computeIfAbsent(tenantId) {
            Sinks.many().multicast().onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false)
        }

    private fun positionSink(tenantId: String): Sinks.Many<PositionUpdate> =
        positionSinks.computeIfAbsent(tenantId) {
            Sinks.many().multicast().onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false)
        }

    override fun emitQueueUpdate(
        tenantId: String,
        update: QueueUpdate,
    ) {
        agentSink(tenantId).tryEmitNext(update)
    }

    override fun emitPositionUpdate(
        tenantId: String,
        update: PositionUpdate,
    ) {
        positionSink(tenantId).tryEmitNext(update)
    }

    override fun subscribeAgentUpdates(tenantId: String): Flux<QueueUpdate> = agentSink(tenantId).asFlux()

    override fun subscribePositionUpdates(tenantId: String): Flux<PositionUpdate> = positionSink(tenantId).asFlux()
}
