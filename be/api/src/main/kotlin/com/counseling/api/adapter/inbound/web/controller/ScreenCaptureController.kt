package com.counseling.api.adapter.inbound.web.controller

import com.counseling.api.adapter.inbound.web.dto.ScreenCaptureListResponse
import com.counseling.api.adapter.inbound.web.dto.ScreenCaptureResponse
import com.counseling.api.domain.ScreenCapture
import com.counseling.api.domain.auth.AuthenticatedAgent
import com.counseling.api.domain.exception.UnauthorizedException
import com.counseling.api.port.inbound.CaptureScreenCommand
import com.counseling.api.port.inbound.ScreenCaptureQuery
import com.counseling.api.port.inbound.ScreenCaptureUseCase
import org.springframework.context.annotation.Profile
import org.springframework.core.io.Resource
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/channels/{channelId}/captures")
@Profile("!test")
class ScreenCaptureController(
    private val screenCaptureUseCase: ScreenCaptureUseCase,
    private val screenCaptureQuery: ScreenCaptureQuery,
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun captureScreen(
        @PathVariable channelId: UUID,
        @RequestPart("image") imagePart: FilePart,
        @RequestPart("note", required = false) note: String?,
    ): Mono<ScreenCaptureResponse> =
        authenticatedAgent().flatMap { agent ->
            DataBufferUtils
                .join(imagePart.content())
                .map { buffer ->
                    val bytes = ByteArray(buffer.readableByteCount())
                    buffer.read(bytes)
                    DataBufferUtils.release(buffer)
                    bytes
                }.flatMap { bytes ->
                    val contentType =
                        imagePart.headers().contentType?.toString() ?: "image/png"
                    screenCaptureUseCase.capture(
                        CaptureScreenCommand(
                            channelId = channelId,
                            capturedBy = agent.agentId,
                            originalFilename = imagePart.filename(),
                            contentType = contentType,
                            fileSize = bytes.size.toLong(),
                            content = bytes,
                            note = note,
                        ),
                    )
                }.map { it.toResponse() }
        }

    @GetMapping
    fun listCaptures(
        @PathVariable channelId: UUID,
        @RequestParam(required = false) before: Instant?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): Mono<ScreenCaptureListResponse> =
        screenCaptureQuery
            .listCaptures(channelId, before, limit.coerceIn(1, 100))
            .map { result ->
                ScreenCaptureListResponse(
                    captures = result.captures.map { it.toResponse() },
                    hasMore = result.hasMore,
                    oldestTimestamp = result.oldestTimestamp,
                )
            }

    @GetMapping("/{captureId}/download")
    fun downloadCapture(
        @PathVariable channelId: UUID,
        @PathVariable captureId: UUID,
    ): Mono<ResponseEntity<Resource>> =
        screenCaptureUseCase
            .download(channelId, captureId)
            .map { captureResource ->
                val encodedName =
                    URLEncoder
                        .encode(captureResource.filename, StandardCharsets.UTF_8)
                        .replace("+", "%20")
                val headers = HttpHeaders()
                headers.contentType = MediaType.parseMediaType(captureResource.contentType)
                headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''$encodedName")
                headers.contentLength = captureResource.contentLength
                ResponseEntity
                    .ok()
                    .headers(headers)
                    .body(captureResource.resource)
            }

    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamCaptureEvents(
        @PathVariable channelId: UUID,
    ): Flux<ScreenCaptureResponse> = screenCaptureQuery.streamCaptureEvents(channelId).map { it.toResponse() }

    @DeleteMapping("/{captureId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCapture(
        @PathVariable channelId: UUID,
        @PathVariable captureId: UUID,
    ): Mono<Void> =
        authenticatedAgent().flatMap {
            screenCaptureUseCase.delete(channelId, captureId)
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

    private fun ScreenCapture.toResponse(): ScreenCaptureResponse =
        ScreenCaptureResponse(
            id = id,
            channelId = channelId,
            capturedBy = capturedBy,
            originalFilename = originalFilename,
            contentType = contentType,
            fileSize = fileSize,
            note = note,
            createdAt = createdAt,
        )
}
