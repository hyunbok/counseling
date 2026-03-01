package com.counseling.api.port.inbound

import com.counseling.api.domain.PositionUpdate
import com.counseling.api.domain.QueueEntry
import com.counseling.api.domain.QueueUpdate
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

data class EnterQueueResult(
    val entry: QueueEntry,
    val position: Long,
    val queueSize: Long,
)

data class AcceptResult(
    val channelId: UUID,
    val customerName: String,
    val customerContact: String,
    val livekitRoomName: String,
    val livekitUrl: String,
    val agentToken: String,
    val customerToken: String,
)

data class PositionResult(
    val position: Long,
    val queueSize: Long,
)

data class QueueEntryWithPosition(
    val entry: QueueEntry,
    val position: Long,
    val waitDurationSeconds: Long,
)

interface QueueUseCase {
    fun enterQueue(
        name: String,
        contact: String,
        groupId: UUID?,
    ): Mono<EnterQueueResult>

    fun leaveQueue(entryId: UUID): Mono<Void>

    fun acceptCustomer(
        entryId: UUID,
        agentId: UUID,
    ): Mono<AcceptResult>

    fun getQueue(): Flux<QueueEntryWithPosition>

    fun getPosition(entryId: UUID): Mono<PositionResult>

    fun subscribeQueueUpdates(): Flux<QueueUpdate>

    fun subscribePositionUpdates(entryId: UUID): Flux<PositionUpdate>
}
