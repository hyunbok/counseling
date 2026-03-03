package com.counseling.api.port.inbound

import com.counseling.api.domain.ScreenCapture
import org.springframework.core.io.Resource
import reactor.core.publisher.Mono
import java.util.UUID

data class CaptureScreenCommand(
    val channelId: UUID,
    val capturedBy: UUID,
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
        channelId: UUID,
        captureId: UUID,
    ): Mono<ScreenCaptureResource>

    fun delete(
        channelId: UUID,
        captureId: UUID,
    ): Mono<Void>
}
