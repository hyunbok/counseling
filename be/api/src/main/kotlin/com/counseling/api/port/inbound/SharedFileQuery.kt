package com.counseling.api.port.inbound

import com.counseling.api.domain.SharedFile
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

data class SharedFileListResult(
    val files: List<SharedFile>,
    val hasMore: Boolean,
    val oldestTimestamp: Instant?,
)

interface SharedFileQuery {
    fun listFiles(
        channelId: UUID,
        before: Instant?,
        limit: Int,
    ): Mono<SharedFileListResult>

    fun streamFileEvents(channelId: UUID): Flux<SharedFile>
}
