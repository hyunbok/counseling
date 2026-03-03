package com.counseling.api.application

import com.counseling.api.domain.ChannelStatus
import com.counseling.api.domain.CoBrowsingSession
import com.counseling.api.domain.CoBrowsingStatus
import com.counseling.api.domain.exception.ConflictException
import com.counseling.api.domain.exception.NotFoundException
import com.counseling.api.port.inbound.CoBrowsingUseCase
import com.counseling.api.port.inbound.EndCoBrowsingCommand
import com.counseling.api.port.inbound.RequestCoBrowsingCommand
import com.counseling.api.port.inbound.StartCoBrowsingCommand
import com.counseling.api.port.outbound.ChannelRepository
import com.counseling.api.port.outbound.CoBrowsingNotificationPort
import com.counseling.api.port.outbound.CoBrowsingSessionReadRepository
import com.counseling.api.port.outbound.CoBrowsingSessionRepository
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Service
@Profile("!test")
class CoBrowsingService(
    private val channelRepository: ChannelRepository,
    private val coBrowsingSessionRepository: CoBrowsingSessionRepository,
    private val coBrowsingSessionReadRepository: CoBrowsingSessionReadRepository,
    private val coBrowsingNotificationPort: CoBrowsingNotificationPort,
) : CoBrowsingUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun requestSession(command: RequestCoBrowsingCommand): Mono<CoBrowsingSession> =
        channelRepository
            .findByIdAndNotDeleted(command.channelId)
            .switchIfEmpty(Mono.error(NotFoundException("Channel not found: ${command.channelId}")))
            .flatMap { channel ->
                if (channel.status != ChannelStatus.IN_PROGRESS) {
                    return@flatMap Mono.error(ConflictException("Channel is not in progress: ${command.channelId}"))
                }
                coBrowsingSessionRepository
                    .findActiveByChannelId(command.channelId)
                    .hasElement()
                    .flatMap { hasActive ->
                        if (hasActive) {
                            return@flatMap Mono.error(
                                ConflictException(
                                    "Co-browsing session already active for channel: ${command.channelId}",
                                ),
                            )
                        }
                        val now = Instant.now()
                        val session =
                            CoBrowsingSession(
                                id = UUID.randomUUID(),
                                channelId = command.channelId,
                                initiatedBy = command.agentId,
                                status = CoBrowsingStatus.REQUESTED,
                                startedAt = null,
                                endedAt = null,
                                createdAt = now,
                                updatedAt = now,
                            )
                        coBrowsingSessionRepository
                            .save(session)
                            .onErrorMap(DataIntegrityViolationException::class.java) {
                                ConflictException(
                                    "Co-browsing session already active for channel: ${command.channelId}",
                                )
                            }.doOnNext { saved ->
                                coBrowsingNotificationPort.emitSessionUpdate(command.channelId, saved)
                            }.flatMap { saved ->
                                coBrowsingSessionReadRepository
                                    .save(saved)
                                    .thenReturn(saved)
                                    .onErrorResume { e ->
                                        log.error(
                                            "Failed to project co-browsing session {} to read store: {}",
                                            saved.id,
                                            e.message,
                                        )
                                        Mono.just(saved)
                                    }
                            }
                    }
            }

    override fun startSession(command: StartCoBrowsingCommand): Mono<CoBrowsingSession> =
        coBrowsingSessionRepository
            .findByIdAndNotDeleted(command.sessionId)
            .switchIfEmpty(Mono.error(NotFoundException("Co-browsing session not found: ${command.sessionId}")))
            .flatMap { session ->
                if (session.channelId != command.channelId) {
                    return@flatMap Mono.error(NotFoundException("Co-browsing session not found: ${command.sessionId}"))
                }
                if (session.status != CoBrowsingStatus.REQUESTED) {
                    return@flatMap Mono.error(
                        ConflictException("Co-browsing session is not in REQUESTED state: ${command.sessionId}"),
                    )
                }
                val updated = session.start()
                coBrowsingSessionRepository
                    .save(updated)
                    .doOnNext { saved ->
                        coBrowsingNotificationPort.emitSessionUpdate(command.channelId, saved)
                    }.flatMap { saved ->
                        coBrowsingSessionReadRepository
                            .updateStatus(
                                id = saved.id,
                                status = saved.status,
                                startedAt = saved.startedAt,
                                endedAt = saved.endedAt,
                                updatedAt = saved.updatedAt,
                            ).thenReturn(saved)
                            .onErrorResume { e ->
                                log.error(
                                    "Failed to update co-browsing session {} in read store: {}",
                                    saved.id,
                                    e.message,
                                )
                                Mono.just(saved)
                            }
                    }
            }

    override fun endSession(command: EndCoBrowsingCommand): Mono<CoBrowsingSession> =
        coBrowsingSessionRepository
            .findByIdAndNotDeleted(command.sessionId)
            .switchIfEmpty(Mono.error(NotFoundException("Co-browsing session not found: ${command.sessionId}")))
            .flatMap { session ->
                if (session.channelId != command.channelId) {
                    return@flatMap Mono.error(NotFoundException("Co-browsing session not found: ${command.sessionId}"))
                }
                if (session.status == CoBrowsingStatus.ENDED) {
                    return@flatMap Mono.error(
                        ConflictException("Co-browsing session already ended: ${command.sessionId}"),
                    )
                }
                val updated = session.end()
                coBrowsingSessionRepository
                    .save(updated)
                    .doOnNext { saved ->
                        coBrowsingNotificationPort.emitSessionUpdate(command.channelId, saved)
                    }.flatMap { saved ->
                        coBrowsingSessionReadRepository
                            .updateStatus(
                                id = saved.id,
                                status = saved.status,
                                startedAt = saved.startedAt,
                                endedAt = saved.endedAt,
                                updatedAt = saved.updatedAt,
                            ).thenReturn(saved)
                            .onErrorResume { e ->
                                log.error(
                                    "Failed to update co-browsing session {} in read store: {}",
                                    saved.id,
                                    e.message,
                                )
                                Mono.just(saved)
                            }
                    }
            }
}
