package com.counseling.api.application

import com.counseling.api.domain.AgentStatus
import com.counseling.api.domain.Channel
import com.counseling.api.domain.ChannelStatus
import com.counseling.api.domain.Endpoint
import com.counseling.api.domain.EndpointType
import com.counseling.api.domain.PositionUpdate
import com.counseling.api.domain.QueueEntry
import com.counseling.api.domain.QueueUpdate
import com.counseling.api.domain.QueueUpdateType
import com.counseling.api.domain.TenantContext
import com.counseling.api.domain.exception.ConflictException
import com.counseling.api.port.inbound.AcceptResult
import com.counseling.api.port.inbound.EnterQueueResult
import com.counseling.api.port.inbound.PositionResult
import com.counseling.api.port.inbound.QueueEntryWithPosition
import com.counseling.api.port.inbound.QueueUseCase
import com.counseling.api.port.outbound.AgentRepository
import com.counseling.api.port.outbound.ChannelRepository
import com.counseling.api.port.outbound.EndpointRepository
import com.counseling.api.port.outbound.QueueNotificationPort
import com.counseling.api.port.outbound.QueueRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Service
@Profile("!test")
class QueueService(
    private val queueRepository: QueueRepository,
    private val queueNotificationPort: QueueNotificationPort,
    private val channelRepository: ChannelRepository,
    private val endpointRepository: EndpointRepository,
    private val agentRepository: AgentRepository,
) : QueueUseCase {
    override fun enterQueue(
        name: String,
        contact: String,
        groupId: UUID?,
    ): Mono<EnterQueueResult> =
        TenantContext.getTenantId().flatMap { tenantId ->
            val entry =
                QueueEntry(
                    id = UUID.randomUUID(),
                    customerName = name,
                    customerContact = contact,
                    groupId = groupId,
                    enteredAt = Instant.now(),
                )
            queueRepository
                .add(tenantId, entry)
                .then(
                    Mono.zip(
                        queueRepository.getPosition(tenantId, entry.id),
                        queueRepository.getSize(tenantId),
                    ),
                ).flatMap { tuple ->
                    val position = tuple.t1
                    val size = tuple.t2
                    val update =
                        QueueUpdate(
                            type = QueueUpdateType.ENTERED,
                            entry = entry,
                            queueSize = size,
                        )
                    queueNotificationPort.emitQueueUpdate(tenantId, update)
                    Mono.just(EnterQueueResult(entry = entry, position = position, queueSize = size))
                }
        }

    override fun leaveQueue(entryId: UUID): Mono<Void> =
        TenantContext.getTenantId().flatMap { tenantId ->
            queueRepository
                .remove(tenantId, entryId)
                .flatMap { entry ->
                    queueRepository.getSize(tenantId).flatMap { size ->
                        val queueUpdate =
                            QueueUpdate(
                                type = QueueUpdateType.LEFT,
                                entry = entry,
                                queueSize = size,
                            )
                        queueNotificationPort.emitQueueUpdate(tenantId, queueUpdate)
                        val positionUpdate =
                            PositionUpdate(
                                entryId = entryId,
                                position = 0L,
                                queueSize = size,
                            )
                        queueNotificationPort.emitPositionUpdate(tenantId, positionUpdate)
                        Mono.empty<Void>()
                    }
                }.switchIfEmpty(Mono.empty())
        }

    override fun acceptCustomer(
        entryId: UUID,
        agentId: UUID,
    ): Mono<AcceptResult> =
        TenantContext.getTenantId().flatMap { tenantId ->
            agentRepository
                .findByIdAndNotDeleted(agentId)
                .switchIfEmpty(Mono.error(ConflictException("Agent not found: $agentId")))
                .flatMap { agent ->
                    if (!agent.isAvailable()) {
                        return@flatMap Mono.error(ConflictException("Agent is not available: $agentId"))
                    }
                    queueRepository
                        .removeAtomically(tenantId, entryId)
                        .flatMap { entry ->
                            val now = Instant.now()
                            val channelId = UUID.randomUUID()
                            val channel =
                                Channel(
                                    id = channelId,
                                    agentId = agentId,
                                    status = ChannelStatus.IN_PROGRESS,
                                    startedAt = now,
                                    endedAt = null,
                                    recordingPath = null,
                                    createdAt = now,
                                    updatedAt = now,
                                )
                            val customerEndpoint =
                                Endpoint(
                                    id = UUID.randomUUID(),
                                    channelId = channelId,
                                    type = EndpointType.CUSTOMER,
                                    customerName = entry.customerName,
                                    customerContact = entry.customerContact,
                                    joinedAt = now,
                                    leftAt = null,
                                )
                            val agentEndpoint =
                                Endpoint(
                                    id = UUID.randomUUID(),
                                    channelId = channelId,
                                    type = EndpointType.AGENT,
                                    customerName = null,
                                    customerContact = null,
                                    joinedAt = now,
                                    leftAt = null,
                                )

                            channelRepository
                                .save(channel)
                                .then(endpointRepository.save(customerEndpoint))
                                .then(endpointRepository.save(agentEndpoint))
                                .then(agentRepository.save(agent.updateStatus(AgentStatus.BUSY)))
                                .flatMap { _ ->
                                    queueRepository.getSize(tenantId).flatMap { size ->
                                        val queueUpdate =
                                            QueueUpdate(
                                                type = QueueUpdateType.ACCEPTED,
                                                entry = entry,
                                                queueSize = size,
                                            )
                                        queueNotificationPort.emitQueueUpdate(tenantId, queueUpdate)
                                        val positionUpdate =
                                            PositionUpdate(
                                                entryId = entryId,
                                                position = 0L,
                                                queueSize = size,
                                            )
                                        queueNotificationPort.emitPositionUpdate(tenantId, positionUpdate)
                                        Mono.just(
                                            AcceptResult(
                                                channelId = channelId,
                                                customerName = entry.customerName,
                                                customerContact = entry.customerContact,
                                            ),
                                        )
                                    }
                                }
                        }
                }
        }

    override fun getQueue(): Flux<QueueEntryWithPosition> =
        TenantContext.getTenantId().flatMapMany { tenantId ->
            queueRepository
                .findAll(tenantId)
                .index()
                .map { indexed ->
                    val position = indexed.t1 + 1
                    val entry = indexed.t2
                    val waitDurationSeconds = (Instant.now().epochSecond - entry.enteredAt.epochSecond)
                    QueueEntryWithPosition(
                        entry = entry,
                        position = position,
                        waitDurationSeconds = waitDurationSeconds,
                    )
                }
        }

    override fun getPosition(entryId: UUID): Mono<PositionResult> =
        TenantContext.getTenantId().flatMap { tenantId ->
            Mono.zip(
                queueRepository.getPosition(tenantId, entryId),
                queueRepository.getSize(tenantId),
            ).map { tuple ->
                PositionResult(position = tuple.t1, queueSize = tuple.t2)
            }
        }

    override fun subscribeQueueUpdates(): Flux<QueueUpdate> =
        TenantContext.getTenantId().flatMapMany { tenantId ->
            queueNotificationPort.subscribeAgentUpdates(tenantId)
        }

    override fun subscribePositionUpdates(entryId: UUID): Flux<PositionUpdate> =
        TenantContext.getTenantId().flatMapMany { tenantId ->
            queueNotificationPort
                .subscribePositionUpdates(tenantId)
                .filter { it.entryId == entryId }
                .takeUntil { it.position == 0L }
        }
}
