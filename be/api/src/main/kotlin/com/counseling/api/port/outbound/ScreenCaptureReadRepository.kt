package com.counseling.api.port.outbound

import com.counseling.api.domain.ScreenCapture
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

interface ScreenCaptureReadRepository {
    fun save(capture: ScreenCapture): Mono<ScreenCapture>

    fun findByChannelId(
        channelId: String,
        before: Instant?,
        limit: Int,
    ): Flux<ScreenCapture>

    fun markDeleted(id: String): Mono<Void>
}
