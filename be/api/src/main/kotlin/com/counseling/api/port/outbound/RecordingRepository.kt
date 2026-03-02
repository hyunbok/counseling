package com.counseling.api.port.outbound

import com.counseling.api.domain.Recording
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface RecordingRepository {
    fun save(recording: Recording): Mono<Recording>

    fun findByIdAndNotDeleted(id: UUID): Mono<Recording>

    fun findActiveByChannelId(channelId: UUID): Mono<Recording>

    fun findAllByChannelIdAndNotDeleted(channelId: UUID): Flux<Recording>
}
