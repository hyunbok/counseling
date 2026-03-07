package com.counseling.api.application

import com.counseling.api.domain.AgentStatus
import com.counseling.api.domain.TenantContext
import com.counseling.api.port.inbound.WebRtcEventHandler
import com.counseling.api.port.outbound.AgentRepository
import com.counseling.api.port.outbound.ChannelRepository
import com.counseling.api.port.outbound.ChatNotificationPort
import com.counseling.api.port.outbound.EndpointRepository
import com.counseling.api.port.outbound.HistoryReadRepository
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration

@Service
@Profile("!test")
class RoomEventService(
    private val channelRepository: ChannelRepository,
    private val endpointRepository: EndpointRepository,
    private val agentRepository: AgentRepository,
    private val historyReadRepository: HistoryReadRepository,
    private val chatNotificationPort: ChatNotificationPort,
) : WebRtcEventHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun onRoomFinished(roomName: String): Mono<Void> {
        val tenantId = extractTenantId(roomName)
        if (tenantId == null) {
            log.warn("Cannot extract tenantId from room name: {}", roomName)
            return Mono.empty()
        }

        return channelRepository
            .findByLivekitRoomNameAndNotDeleted(roomName)
            .contextWrite { ctx -> TenantContext.withTenantId(ctx, tenantId) }
            .flatMap { channel ->
                if (!channel.isOpen()) {
                    log.debug("Channel {} already closed, skipping", channel.id)
                    return@flatMap Mono.empty<Void>()
                }

                log.info("Auto-closing channel {} (room: {})", channel.id, roomName)
                val closedChannel = channel.close()
                val durationSeconds =
                    if (closedChannel.startedAt != null && closedChannel.endedAt != null) {
                        closedChannel.endedAt.epochSecond - closedChannel.startedAt.epochSecond
                    } else {
                        null
                    }

                endpointRepository
                    .findAllByChannelId(channel.id)
                    .flatMap { endpoint -> endpointRepository.save(endpoint.leave()) }
                    .then(channelRepository.save(closedChannel))
                    .flatMap {
                        historyReadRepository
                            .updateStatus(
                                channelId = channel.id,
                                tenantId = tenantId,
                                status = "CLOSED",
                                endedAt = closedChannel.endedAt,
                                durationSeconds = durationSeconds,
                            ).timeout(Duration.ofSeconds(5))
                            .onErrorResume { e ->
                                log.error(
                                    "Failed to update history for channel {}: {}",
                                    channel.id,
                                    e.message,
                                )
                                Mono.empty()
                            }
                    }.then(
                        channel.agentId?.let { agentId ->
                            agentRepository
                                .findByIdAndNotDeleted(agentId)
                                .flatMap { agent ->
                                    agentRepository.save(agent.updateStatus(AgentStatus.ONLINE))
                                }.then()
                        } ?: Mono.empty(),
                    ).doOnSuccess {
                        chatNotificationPort.removeChannel(channel.id)
                    }.then()
                    .contextWrite { ctx -> TenantContext.withTenantId(ctx, tenantId) }
            }
    }

    private fun extractTenantId(roomName: String): String? {
        val idx = roomName.indexOf("-channel-")
        return if (idx > 0) roomName.substring(0, idx) else null
    }
}
