package com.counseling.api.port.inbound

import com.counseling.api.domain.PositionUpdate
import com.counseling.api.domain.QueueEntry
import com.counseling.api.domain.QueueUpdate
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

data class EnterQueueResult(
    val entry: QueueEntry,
    val position: Long,
    val queueSize: Long,
)

data class AcceptResult(
    val channelId: String,
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
        groupId: String?,
    ): Mono<EnterQueueResult>

    fun leaveQueue(entryId: String): Mono<Void>

    fun acceptCustomer(
        entryId: String,
        agentId: String,
    ): Mono<AcceptResult>

    fun getQueue(): Flux<QueueEntryWithPosition>

    fun getPosition(entryId: String): Mono<PositionResult>

    fun subscribeQueueUpdates(): Flux<QueueUpdate>

    fun subscribePositionUpdates(entryId: String): Flux<PositionUpdate>
}
