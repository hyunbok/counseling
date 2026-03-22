package com.counseling.api.port.outbound

import com.counseling.api.domain.CounselNote
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface CounselNoteRepository : ReactiveCrudRepository<CounselNote, String> {
    fun findByIdAndDeletedFalse(id: String): Mono<CounselNote>

    fun findAllByChannelIdAndDeletedFalseOrderByCreatedAt(channelId: String): Flux<CounselNote>
}
