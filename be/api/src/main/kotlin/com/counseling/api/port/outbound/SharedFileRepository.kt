package com.counseling.api.port.outbound

import com.counseling.api.domain.SharedFile
import reactor.core.publisher.Mono

interface SharedFileRepository {
    fun save(file: SharedFile): Mono<SharedFile>

    fun findByIdAndNotDeleted(id: String): Mono<SharedFile>

    fun softDelete(id: String): Mono<Void>
}
