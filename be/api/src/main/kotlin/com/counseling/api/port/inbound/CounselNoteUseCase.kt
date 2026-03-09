package com.counseling.api.port.inbound

import com.counseling.api.domain.CounselNote
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

data class SaveCounselNoteCommand(
    val channelId: UUID,
    val agentId: UUID,
    val tenantId: String,
    val content: String,
)

interface CounselNoteUseCase {
    fun save(command: SaveCounselNoteCommand): Mono<CounselNote>

    fun findByChannel(channelId: UUID): Flux<CounselNote>
}
