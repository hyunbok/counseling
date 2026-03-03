package com.counseling.api.port.inbound

import com.counseling.api.domain.ScreenCapture
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

data class ScreenCaptureListResult(
    val captures: List<ScreenCapture>,
    val hasMore: Boolean,
    val oldestTimestamp: Instant?,
)

interface ScreenCaptureQuery {
    fun listCaptures(
        channelId: UUID,
        before: Instant?,
        limit: Int,
    ): Mono<ScreenCaptureListResult>

    fun streamCaptureEvents(channelId: UUID): Flux<ScreenCapture>
}
