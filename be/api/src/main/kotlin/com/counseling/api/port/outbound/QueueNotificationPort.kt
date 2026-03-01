package com.counseling.api.port.outbound

import com.counseling.api.domain.PositionUpdate
import com.counseling.api.domain.QueueUpdate
import reactor.core.publisher.Flux

interface QueueNotificationPort {
    fun emitQueueUpdate(
        tenantId: String,
        update: QueueUpdate,
    )

    fun emitPositionUpdate(
        tenantId: String,
        update: PositionUpdate,
    )

    fun subscribeAgentUpdates(tenantId: String): Flux<QueueUpdate>

    fun subscribePositionUpdates(tenantId: String): Flux<PositionUpdate>
}
