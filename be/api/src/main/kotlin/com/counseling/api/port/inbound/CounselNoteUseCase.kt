package com.counseling.api.port.inbound

import com.counseling.api.domain.CounselNote
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

data class SaveCounselNoteCommand(
    val channelId: String,
    val agentId: String,
    val tenantId: String,
    val content: String,
)

interface CounselNoteUseCase {
    fun save(command: SaveCounselNoteCommand): Mono<CounselNote>

    fun findByChannel(channelId: String): Flux<CounselNote>
}
