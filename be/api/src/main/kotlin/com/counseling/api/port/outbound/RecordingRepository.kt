package com.counseling.api.port.outbound

import com.counseling.api.domain.Recording
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface RecordingRepository {
    fun save(recording: Recording): Mono<Recording>

    fun findByIdAndNotDeleted(id: String): Mono<Recording>

    fun findActiveByChannelId(channelId: String): Mono<Recording>

    fun findAllByChannelIdAndNotDeleted(channelId: String): Flux<Recording>
}
