package com.counseling.api.port.inbound

import com.counseling.api.domain.ScreenCapture
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

data class ScreenCaptureListResult(
    val captures: List<ScreenCapture>,
    val hasMore: Boolean,
    val oldestTimestamp: Instant?,
)

interface ScreenCaptureQuery {
    fun listCaptures(
        channelId: String,
        before: Instant?,
        limit: Int,
    ): Mono<ScreenCaptureListResult>

    fun streamCaptureEvents(channelId: String): Flux<ScreenCapture>
}
