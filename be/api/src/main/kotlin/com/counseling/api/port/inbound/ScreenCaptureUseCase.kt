package com.counseling.api.port.inbound

import com.counseling.api.domain.ScreenCapture
import org.springframework.core.io.Resource
import reactor.core.publisher.Mono

data class CaptureScreenCommand(
    val channelId: String,
    val capturedBy: String,
    val originalFilename: String,
    val contentType: String,
    val fileSize: Long,
    val content: ByteArray,
    val note: String?,
)

data class ScreenCaptureResource(
    val resource: Resource,
    val filename: String,
    val contentType: String,
    val contentLength: Long,
)

interface ScreenCaptureUseCase {
    fun capture(command: CaptureScreenCommand): Mono<ScreenCapture>

    fun download(
        channelId: String,
        captureId: String,
    ): Mono<ScreenCaptureResource>

    fun delete(
        channelId: String,
        captureId: String,
    ): Mono<Void>
}
