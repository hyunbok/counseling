package com.counseling.api.port.outbound

import com.counseling.api.domain.ScreenCapture
import reactor.core.publisher.Flux

interface CaptureNotificationPort {
    fun emitCapture(
        channelId: String,
        capture: ScreenCapture,
    )

    fun subscribeCaptures(channelId: String): Flux<ScreenCapture>

    fun removeChannel(channelId: String)
}
