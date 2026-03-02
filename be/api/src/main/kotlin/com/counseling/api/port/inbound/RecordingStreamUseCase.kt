package com.counseling.api.port.inbound

import org.springframework.core.io.Resource
import reactor.core.publisher.Mono
import java.util.UUID

data class RecordingResource(
    val resource: Resource,
    val contentLength: Long,
    val filename: String,
)

interface RecordingStreamUseCase {
    fun getRecordingResource(
        channelId: UUID,
        recordingId: UUID,
        agentId: UUID,
    ): Mono<RecordingResource>
}
