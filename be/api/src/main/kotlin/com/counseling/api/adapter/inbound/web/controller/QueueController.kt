package com.counseling.api.adapter.inbound.web.controller

import com.counseling.api.adapter.inbound.web.dto.AcceptResponse
import com.counseling.api.adapter.inbound.web.dto.EnterQueueRequest
import com.counseling.api.adapter.inbound.web.dto.EnterQueueResponse
import com.counseling.api.adapter.inbound.web.dto.PositionResponse
import com.counseling.api.adapter.inbound.web.dto.PositionUpdateEvent
import com.counseling.api.adapter.inbound.web.dto.QueueEntryView
import com.counseling.api.adapter.inbound.web.dto.QueueUpdateEvent
import com.counseling.api.domain.auth.AuthenticatedAgent
import com.counseling.api.domain.exception.UnauthorizedException
import com.counseling.api.port.inbound.QueueUseCase
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/api/queue")
@Profile("!test")
class QueueController(
    private val queueUseCase: QueueUseCase,
) {
    @PostMapping("/enter")
    @ResponseStatus(HttpStatus.CREATED)
    fun enter(
        @RequestBody request: EnterQueueRequest,
    ): Mono<EnterQueueResponse> =
        queueUseCase
            .enterQueue(request.name, request.contact, request.groupId)
            .map { result ->
                EnterQueueResponse(
                    entryId = result.entry.id,
                    position = result.position,
                    queueSize = result.queueSize,
                )
            }

    @DeleteMapping("/{entryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun leave(
        @PathVariable entryId: UUID,
    ): Mono<Void> = queueUseCase.leaveQueue(entryId)

    @GetMapping
    fun getQueue(): Flux<QueueEntryView> =
        queueUseCase.getQueue().map { item ->
            QueueEntryView(
                entryId = item.entry.id,
                name = item.entry.customerName,
                contact = item.entry.customerContact,
                groupId = item.entry.groupId,
                enteredAt = item.entry.enteredAt,
                waitDurationSeconds = item.waitDurationSeconds,
                position = item.position,
            )
        }

    @PostMapping("/{entryId}/accept")
    fun accept(
        @PathVariable entryId: UUID,
    ): Mono<AcceptResponse> =
        authenticatedAgent().flatMap { principal ->
            queueUseCase
                .acceptCustomer(entryId, principal.agentId)
                .map { result ->
                    AcceptResponse(
                        channelId = result.channelId,
                        customerName = result.customerName,
                        customerContact = result.customerContact,
                        livekitRoomName = result.livekitRoomName,
                        livekitUrl = result.livekitUrl,
                        agentToken = result.agentToken,
                        customerToken = result.customerToken,
                    )
                }
        }

    @GetMapping("/position/{entryId}")
    fun getPosition(
        @PathVariable entryId: UUID,
    ): Mono<PositionResponse> =
        queueUseCase.getPosition(entryId).map { result ->
            PositionResponse(
                entryId = entryId,
                position = result.position,
                queueSize = result.queueSize,
            )
        }

    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamQueue(): Flux<QueueUpdateEvent> =
        queueUseCase.subscribeQueueUpdates().map { update ->
            QueueUpdateEvent(
                type = update.type.name,
                entryId = update.entry?.id,
                customerName = update.entry?.customerName,
                queueSize = update.queueSize,
                timestamp = update.timestamp,
            )
        }

    @GetMapping("/position/{entryId}/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamPosition(
        @PathVariable entryId: UUID,
    ): Flux<PositionUpdateEvent> =
        queueUseCase.subscribePositionUpdates(entryId).map { update ->
            PositionUpdateEvent(
                entryId = update.entryId,
                position = update.position,
                queueSize = update.queueSize,
                timestamp = update.timestamp,
            )
        }

    private fun authenticatedAgent(): Mono<AuthenticatedAgent> =
        ReactiveSecurityContextHolder
            .getContext()
            .flatMap { ctx ->
                val principal = ctx.authentication?.principal
                if (principal is AuthenticatedAgent) {
                    Mono.just(principal)
                } else {
                    Mono.error(UnauthorizedException("Not authenticated"))
                }
            }
}
