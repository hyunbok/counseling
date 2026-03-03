package com.counseling.api.port.outbound

import com.counseling.api.domain.ScreenCapture
import reactor.core.publisher.Flux
import java.util.UUID

interface CaptureNotificationPort {
    fun emitCapture(
        channelId: UUID,
        capture: ScreenCapture,
    )

    fun subscribeCaptures(channelId: UUID): Flux<ScreenCapture>

    fun removeChannel(channelId: UUID)
}
