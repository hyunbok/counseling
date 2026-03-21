package com.counseling.api.port.outbound

import com.counseling.api.domain.CounselNote
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface CounselNoteRepository {
    fun save(note: CounselNote): Mono<CounselNote>

    fun findByIdAndNotDeleted(id: String): Mono<CounselNote>

    fun findAllByChannelIdAndNotDeleted(channelId: String): Flux<CounselNote>
}
