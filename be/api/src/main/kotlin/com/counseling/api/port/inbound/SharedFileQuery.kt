package com.counseling.api.port.inbound

import com.counseling.api.domain.SharedFile
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

data class SharedFileListResult(
    val files: List<SharedFile>,
    val hasMore: Boolean,
    val oldestTimestamp: Instant?,
)

interface SharedFileQuery {
    fun listFiles(
        channelId: String,
        before: Instant?,
        limit: Int,
    ): Mono<SharedFileListResult>

    fun streamFileEvents(channelId: String): Flux<SharedFile>
}
