package com.counseling.api.port.outbound

import com.counseling.api.domain.ScreenCapture
import reactor.core.publisher.Mono
import java.util.UUID

interface ScreenCaptureRepository {
    fun save(capture: ScreenCapture): Mono<ScreenCapture>

    fun findByIdAndNotDeleted(id: UUID): Mono<ScreenCapture>

    fun softDelete(id: UUID): Mono<Void>
}
