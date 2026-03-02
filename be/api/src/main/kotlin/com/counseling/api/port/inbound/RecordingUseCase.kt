package com.counseling.api.port.inbound

import com.counseling.api.domain.RecordingStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

data class StartRecordingResult(
    val recordingId: UUID,
    val channelId: UUID,
    val egressId: String,
    val status: RecordingStatus,
    val startedAt: Instant,
)

data class StopRecordingResult(
    val recordingId: UUID,
    val channelId: UUID,
    val egressId: String,
    val status: RecordingStatus,
    val startedAt: Instant,
    val stoppedAt: Instant?,
    val filePath: String?,
)

data class RecordingInfo(
    val recordingId: UUID,
    val channelId: UUID,
    val egressId: String,
    val status: RecordingStatus,
    val startedAt: Instant,
    val stoppedAt: Instant?,
    val filePath: String?,
)

interface RecordingUseCase {
    fun startRecording(
        channelId: UUID,
        agentId: UUID,
    ): Mono<StartRecordingResult>

    fun stopRecording(
        channelId: UUID,
        agentId: UUID,
    ): Mono<StopRecordingResult>

    fun getRecordings(
        channelId: UUID,
        agentId: UUID,
    ): Flux<RecordingInfo>
}
