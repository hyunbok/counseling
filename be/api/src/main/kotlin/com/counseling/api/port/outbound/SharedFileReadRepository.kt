package com.counseling.api.port.outbound

import com.counseling.api.domain.SharedFile
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

interface SharedFileReadRepository {
    fun save(file: SharedFile): Mono<SharedFile>

    fun findByChannelId(
        channelId: UUID,
        before: Instant?,
        limit: Int,
    ): Flux<SharedFile>

    fun markDeleted(id: UUID): Mono<Void>
}
