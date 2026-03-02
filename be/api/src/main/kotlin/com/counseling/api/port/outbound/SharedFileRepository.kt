package com.counseling.api.port.outbound

import com.counseling.api.domain.SharedFile
import reactor.core.publisher.Mono
import java.util.UUID

interface SharedFileRepository {
    fun save(file: SharedFile): Mono<SharedFile>

    fun findByIdAndNotDeleted(id: UUID): Mono<SharedFile>

    fun softDelete(id: UUID): Mono<Void>
}
