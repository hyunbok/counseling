package com.counseling.api.port.outbound

import com.counseling.api.domain.CounselNote
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface CounselNoteRepository {
    fun save(note: CounselNote): Mono<CounselNote>

    fun findByIdAndNotDeleted(id: UUID): Mono<CounselNote>

    fun findAllByChannelIdAndNotDeleted(channelId: UUID): Flux<CounselNote>
}
