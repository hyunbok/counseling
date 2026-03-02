package com.counseling.api.adapter.inbound.web.controller

import com.counseling.api.adapter.inbound.web.dto.RecordingInfoResponse
import com.counseling.api.adapter.inbound.web.dto.RecordingListResponse
import com.counseling.api.adapter.inbound.web.dto.StartRecordingResponse
import com.counseling.api.adapter.inbound.web.dto.StopRecordingResponse
import com.counseling.api.domain.auth.AuthenticatedAgent
import com.counseling.api.domain.exception.UnauthorizedException
import com.counseling.api.port.inbound.RecordingUseCase
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/api/channels/{channelId}/recordings")
@Profile("!test")
class RecordingController(
    private val recordingUseCase: RecordingUseCase,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun startRecording(
        @PathVariable channelId: UUID,
    ): Mono<StartRecordingResponse> =
        authenticatedAgent().flatMap { principal ->
            recordingUseCase
                .startRecording(channelId, principal.agentId)
                .map { result ->
                    StartRecordingResponse(
                        recordingId = result.recordingId,
                        channelId = result.channelId,
                        egressId = result.egressId,
                        status = result.status.name,
                        startedAt = result.startedAt,
                    )
                }
        }

    @PostMapping("/stop")
    fun stopRecording(
        @PathVariable channelId: UUID,
    ): Mono<StopRecordingResponse> =
        authenticatedAgent().flatMap { principal ->
            recordingUseCase
                .stopRecording(channelId, principal.agentId)
                .map { result ->
                    StopRecordingResponse(
                        recordingId = result.recordingId,
                        channelId = result.channelId,
                        egressId = result.egressId,
                        status = result.status.name,
                        startedAt = result.startedAt,
                        stoppedAt = result.stoppedAt,
                        filePath = result.filePath,
                    )
                }
        }

    @GetMapping
    fun getRecordings(
        @PathVariable channelId: UUID,
    ): Mono<RecordingListResponse> =
        authenticatedAgent().flatMap { principal ->
            recordingUseCase
                .getRecordings(channelId, principal.agentId)
                .map { info ->
                    RecordingInfoResponse(
                        recordingId = info.recordingId,
                        channelId = info.channelId,
                        egressId = info.egressId,
                        status = info.status.name,
                        startedAt = info.startedAt,
                        stoppedAt = info.stoppedAt,
                        filePath = info.filePath,
                    )
                }.collectList()
                .map { list -> RecordingListResponse(recordings = list) }
        }

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
}
