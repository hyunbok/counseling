package com.counseling.api.adapter.inbound.web.controller

import com.counseling.api.domain.CounselNote
import com.counseling.api.domain.auth.AuthenticatedAgent
import com.counseling.api.domain.exception.UnauthorizedException
import com.counseling.api.port.inbound.CounselNoteUseCase
import com.counseling.api.port.inbound.SaveCounselNoteCommand
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

data class SaveNoteRequest(
    val content: String,
)

data class CounselNoteResponse(
    val id: UUID,
    val channelId: UUID,
    val agentId: UUID,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@RestController
@RequestMapping("/api/channels/{channelId}/notes")
@Profile("!test")
class CounselNoteController(
    private val counselNoteUseCase: CounselNoteUseCase,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    fun saveNote(
        @PathVariable channelId: UUID,
        @RequestBody request: SaveNoteRequest,
    ): Mono<CounselNoteResponse> =
        authenticatedAgent().flatMap { agent ->
            counselNoteUseCase
                .save(
                    SaveCounselNoteCommand(
                        channelId = channelId,
                        agentId = agent.agentId,
                        tenantId = agent.tenantId,
                        content = request.content,
                    ),
                ).map { it.toResponse() }
        }

    @GetMapping
    fun getNotes(
        @PathVariable channelId: UUID,
    ): Mono<List<CounselNoteResponse>> =
        counselNoteUseCase
            .findByChannel(channelId)
            .map { it.toResponse() }
            .collectList()

    private fun authenticatedAgent(): Mono<AuthenticatedAgent> =
        ReactiveSecurityContextHolder
            .getContext()
            .flatMap { ctx ->
                val principal = ctx.authentication?.principal
                if (principal is AuthenticatedAgent) {
                    Mono.just(principal)
                } else {
                    Mono.error(UnauthorizedException("Not authenticated"))
                }
            }

    private fun CounselNote.toResponse(): CounselNoteResponse =
        CounselNoteResponse(
            id = id,
            channelId = channelId,
            agentId = agentId,
            content = content,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
