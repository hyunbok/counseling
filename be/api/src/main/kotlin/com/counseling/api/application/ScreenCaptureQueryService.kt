package com.counseling.api.application

import com.counseling.api.domain.ScreenCapture
import com.counseling.api.port.inbound.ScreenCaptureListResult
import com.counseling.api.port.inbound.ScreenCaptureQuery
import com.counseling.api.port.outbound.CaptureNotificationPort
import com.counseling.api.port.outbound.ScreenCaptureReadRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Service
@Profile("!test")
class ScreenCaptureQueryService(
    private val screenCaptureReadRepository: ScreenCaptureReadRepository,
    private val captureNotificationPort: CaptureNotificationPort,
) : ScreenCaptureQuery {
    override fun listCaptures(
        channelId: UUID,
        before: Instant?,
        limit: Int,
    ): Mono<ScreenCaptureListResult> =
        screenCaptureReadRepository
            .findByChannelId(channelId, before, limit + 1)
            .collectList()
            .map { captures ->
                val hasMore = captures.size > limit
                val trimmed = if (hasMore) captures.dropLast(1) else captures
                val reversed = trimmed.reversed()
                ScreenCaptureListResult(
                    captures = reversed,
                    hasMore = hasMore,
                    oldestTimestamp = reversed.firstOrNull()?.createdAt,
                )
            }

    override fun streamCaptureEvents(channelId: UUID): Flux<ScreenCapture> =
        captureNotificationPort.subscribeCaptures(channelId)
}
