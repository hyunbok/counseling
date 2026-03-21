package com.counseling.api.port.inbound

import org.springframework.core.io.Resource
import reactor.core.publisher.Mono

data class RecordingResource(
    val resource: Resource,
    val contentLength: Long,
    val filename: String,
)

interface RecordingStreamUseCase {
    fun getRecordingResource(
        channelId: String,
        recordingId: String,
        agentId: String,
    ): Mono<RecordingResource>
}
