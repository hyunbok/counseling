package com.counseling.api.application

import com.counseling.api.config.RecordingProperties
import com.counseling.api.domain.RecordingStatus
import com.counseling.api.domain.exception.ConflictException
import com.counseling.api.domain.exception.NotFoundException
import com.counseling.api.domain.exception.UnauthorizedException
import com.counseling.api.port.inbound.RecordingResource
import com.counseling.api.port.inbound.RecordingStreamUseCase
import com.counseling.api.port.outbound.ChannelRepository
import com.counseling.api.port.outbound.RecordingRepository
import org.springframework.context.annotation.Profile
import org.springframework.core.io.FileSystemResource
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.io.File
import java.util.UUID

@Service
@Profile("!test")
class RecordingStreamService(
    private val recordingRepository: RecordingRepository,
    private val channelRepository: ChannelRepository,
    private val recordingProperties: RecordingProperties,
) : RecordingStreamUseCase {
    override fun getRecordingResource(
        channelId: UUID,
        recordingId: UUID,
        agentId: UUID,
    ): Mono<RecordingResource> =
        recordingRepository
            .findByIdAndNotDeleted(recordingId)
            .switchIfEmpty(Mono.error(NotFoundException("Recording not found: $recordingId")))
            .flatMap { recording ->
                if (recording.channelId != channelId) {
                    return@flatMap Mono.error(
                        ConflictException("Recording $recordingId does not belong to channel $channelId"),
                    )
                }
                if (recording.status != RecordingStatus.STOPPED) {
                    return@flatMap Mono.error(
                        ConflictException("Recording $recordingId is not yet complete"),
                    )
                }
                channelRepository
                    .findByIdAndNotDeleted(channelId)
                    .switchIfEmpty(Mono.error(NotFoundException("Channel not found: $channelId")))
                    .flatMap { channel ->
                        if (channel.agentId != agentId) {
                            return@flatMap Mono.error(
                                UnauthorizedException(
                                    "Agent $agentId is not authorized to access recording $recordingId",
                                ),
                            )
                        }
                        val filePath =
                            recording.filePath
                                ?: return@flatMap Mono.error(
                                    ConflictException("Recording $recordingId has no file path"),
                                )
                        val file = File(filePath)
                        if (!file.exists()) {
                            return@flatMap Mono.error(NotFoundException("Recording file not found: $filePath"))
                        }
                        val resource = FileSystemResource(file)
                        Mono.just(
                            RecordingResource(
                                resource = resource,
                                contentLength = file.length(),
                                filename = file.name,
                            ),
                        )
                    }
            }
}
