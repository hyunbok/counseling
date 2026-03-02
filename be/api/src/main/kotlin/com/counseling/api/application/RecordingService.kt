package com.counseling.api.application

import com.counseling.api.config.RecordingProperties
import com.counseling.api.domain.ChannelStatus
import com.counseling.api.domain.Recording
import com.counseling.api.domain.RecordingStatus
import com.counseling.api.domain.TenantContext
import com.counseling.api.domain.exception.ConflictException
import com.counseling.api.domain.exception.NotFoundException
import com.counseling.api.port.inbound.RecordingInfo
import com.counseling.api.port.inbound.RecordingUseCase
import com.counseling.api.port.inbound.StartRecordingResult
import com.counseling.api.port.inbound.StopRecordingResult
import com.counseling.api.port.outbound.ChannelRepository
import com.counseling.api.port.outbound.LiveKitEgressPort
import com.counseling.api.port.outbound.RecordingRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Service
@Profile("!test")
class RecordingService(
    private val recordingRepository: RecordingRepository,
    private val channelRepository: ChannelRepository,
    private val liveKitEgressPort: LiveKitEgressPort,
    private val recordingProperties: RecordingProperties,
) : RecordingUseCase {
    override fun startRecording(
        channelId: UUID,
        agentId: UUID,
    ): Mono<StartRecordingResult> =
        TenantContext.getTenantId().flatMap { tenantId ->
            channelRepository
                .findByIdAndNotDeleted(channelId)
                .switchIfEmpty(Mono.error(NotFoundException("Channel not found: $channelId")))
                .flatMap { channel ->
                    if (channel.status != ChannelStatus.IN_PROGRESS) {
                        return@flatMap Mono.error(ConflictException("Channel is not in progress: $channelId"))
                    }
                    if (channel.agentId != agentId) {
                        return@flatMap Mono.error(ConflictException("Not authorized"))
                    }
                    val roomName =
                        channel.livekitRoomName
                            ?: return@flatMap Mono.error(ConflictException("No LiveKit room"))
                    recordingRepository
                        .findActiveByChannelId(channelId)
                        .hasElement()
                        .flatMap { hasActive ->
                            if (hasActive) {
                                return@flatMap Mono.error(ConflictException("Already recording"))
                            }
                            val recordingId = UUID.randomUUID()
                            val filePath =
                                "${recordingProperties.basePath}/$tenantId/$recordingId.${recordingProperties.fileFormat}"
                            liveKitEgressPort
                                .startRoomCompositeEgress(roomName, filePath)
                                .flatMap { egressResult ->
                                    val now = Instant.now()
                                    val recording =
                                        Recording(
                                            id = recordingId,
                                            channelId = channelId,
                                            egressId = egressResult.egressId,
                                            status = RecordingStatus.RECORDING,
                                            filePath = filePath,
                                            startedAt = now,
                                            stoppedAt = null,
                                            createdAt = now,
                                            updatedAt = now,
                                        )
                                    recordingRepository.save(recording).map { saved ->
                                        StartRecordingResult(
                                            recordingId = saved.id,
                                            channelId = saved.channelId,
                                            egressId = saved.egressId,
                                            status = saved.status,
                                            startedAt = saved.startedAt,
                                        )
                                    }
                                }
                        }
                }
        }

    override fun stopRecording(
        channelId: UUID,
        agentId: UUID,
    ): Mono<StopRecordingResult> =
        channelRepository
            .findByIdAndNotDeleted(channelId)
            .switchIfEmpty(Mono.error(NotFoundException("Channel not found: $channelId")))
            .flatMap { channel ->
                if (channel.status != ChannelStatus.IN_PROGRESS) {
                    return@flatMap Mono.error(ConflictException("Channel is not in progress: $channelId"))
                }
                if (channel.agentId != agentId) {
                    return@flatMap Mono.error(ConflictException("Not authorized"))
                }
                recordingRepository
                    .findActiveByChannelId(channelId)
                    .switchIfEmpty(Mono.error(NotFoundException("No active recording for channel: $channelId")))
                    .flatMap { recording ->
                        liveKitEgressPort
                            .stopEgress(recording.egressId)
                            .then(
                                Mono.defer {
                                    val stopped = recording.stop(recording.filePath)
                                    recordingRepository.save(stopped).flatMap { savedRecording ->
                                        val updatedChannel =
                                            if (savedRecording.filePath != null) {
                                                channel.withRecordingPath(savedRecording.filePath)
                                            } else {
                                                channel
                                            }
                                        channelRepository.save(updatedChannel).thenReturn(savedRecording)
                                    }
                                },
                            ).map { saved ->
                                StopRecordingResult(
                                    recordingId = saved.id,
                                    channelId = saved.channelId,
                                    egressId = saved.egressId,
                                    status = saved.status,
                                    startedAt = saved.startedAt,
                                    stoppedAt = saved.stoppedAt,
                                    filePath = saved.filePath,
                                )
                            }
                    }
            }

    override fun getRecordings(
        channelId: UUID,
        agentId: UUID,
    ): Flux<RecordingInfo> =
        channelRepository
            .findByIdAndNotDeleted(channelId)
            .switchIfEmpty(Mono.error(NotFoundException("Channel not found: $channelId")))
            .flatMap { channel ->
                if (channel.agentId != agentId) {
                    return@flatMap Mono.error(ConflictException("Not authorized"))
                }
                Mono.just(channel)
            }.flatMapMany { _ ->
                recordingRepository
                    .findAllByChannelIdAndNotDeleted(channelId)
                    .map { recording ->
                        RecordingInfo(
                            recordingId = recording.id,
                            channelId = recording.channelId,
                            egressId = recording.egressId,
                            status = recording.status,
                            startedAt = recording.startedAt,
                            stoppedAt = recording.stoppedAt,
                            filePath = recording.filePath,
                        )
                    }
            }
}
