package com.counseling.api.port.outbound

import com.counseling.api.domain.SharedFile
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

interface SharedFileReadRepository {
    fun save(file: SharedFile): Mono<SharedFile>

    fun findByChannelId(
        channelId: String,
        before: Instant?,
        limit: Int,
    ): Flux<SharedFile>

    fun markDeleted(id: String): Mono<Void>
}
