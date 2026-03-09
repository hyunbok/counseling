package com.counseling.api.application

import com.counseling.api.domain.CounselNote
import com.counseling.api.port.inbound.CounselNoteUseCase
import com.counseling.api.port.inbound.SaveCounselNoteCommand
import com.counseling.api.port.outbound.CounselNoteProjection
import com.counseling.api.port.outbound.CounselNoteRepository
import com.counseling.api.port.outbound.HistoryReadRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Service
class CounselNoteService(
    private val counselNoteRepository: CounselNoteRepository,
    private val historyReadRepository: HistoryReadRepository,
) : CounselNoteUseCase {
    override fun save(command: SaveCounselNoteCommand): Mono<CounselNote> =
        counselNoteRepository
            .findAllByChannelIdAndNotDeleted(command.channelId)
            .filter { it.agentId == command.agentId }
            .next()
            .flatMap { existing ->
                counselNoteRepository.save(existing.updateContent(command.content))
            }.switchIfEmpty(
                Mono.defer {
                    val now = Instant.now()
                    counselNoteRepository.save(
                        CounselNote(
                            id = UUID.randomUUID(),
                            channelId = command.channelId,
                            agentId = command.agentId,
                            content = command.content,
                            createdAt = now,
                            updatedAt = now,
                        ),
                    )
                },
            ).flatMap { savedNote ->
                historyReadRepository
                    .updateCounselNote(
                        channelId = command.channelId,
                        tenantId = command.tenantId,
                        counselNote =
                            CounselNoteProjection(
                                noteId = savedNote.id,
                                content = savedNote.content,
                                createdAt = savedNote.createdAt,
                                updatedAt = savedNote.updatedAt,
                            ),
                    ).thenReturn(savedNote)
            }

    override fun findByChannel(channelId: UUID): Flux<CounselNote> =
        counselNoteRepository.findAllByChannelIdAndNotDeleted(channelId)
}
