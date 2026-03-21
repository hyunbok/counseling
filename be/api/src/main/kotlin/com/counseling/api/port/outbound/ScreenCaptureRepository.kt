package com.counseling.api.port.outbound

import com.counseling.api.domain.ScreenCapture
import reactor.core.publisher.Mono

interface ScreenCaptureRepository {
    fun save(capture: ScreenCapture): Mono<ScreenCapture>

    fun findByIdAndNotDeleted(id: String): Mono<ScreenCapture>

    fun softDelete(id: String): Mono<Void>
}
