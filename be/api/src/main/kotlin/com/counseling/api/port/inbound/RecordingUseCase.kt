package com.counseling.api.port.inbound

import com.counseling.api.domain.RecordingStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

data class StartRecordingResult(
    val recordingId: String,
    val channelId: String,
    val egressId: String,
    val status: RecordingStatus,
    val startedAt: Instant,
)

data class StopRecordingResult(
    val recordingId: String,
    val channelId: String,
    val egressId: String,
    val status: RecordingStatus,
    val startedAt: Instant,
    val stoppedAt: Instant?,
    val filePath: String?,
)

data class RecordingInfo(
    val recordingId: String,
    val channelId: String,
    val egressId: String,
    val status: RecordingStatus,
    val startedAt: Instant,
    val stoppedAt: Instant?,
    val filePath: String?,
)

interface RecordingUseCase {
    fun startRecording(
        channelId: String,
        agentId: String,
    ): Mono<StartRecordingResult>

    fun stopRecording(
        channelId: String,
        agentId: String,
    ): Mono<StopRecordingResult>

    fun getRecordings(
        channelId: String,
        agentId: String,
    ): Flux<RecordingInfo>
}
