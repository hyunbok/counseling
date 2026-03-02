package com.counseling.api.adapter.inbound.web.controller

import com.counseling.api.adapter.inbound.web.dto.SharedFileListResponse
import com.counseling.api.adapter.inbound.web.dto.SharedFileResponse
import com.counseling.api.domain.SenderType
import com.counseling.api.domain.SharedFile
import com.counseling.api.port.inbound.SharedFileQuery
import com.counseling.api.port.inbound.SharedFileUseCase
import com.counseling.api.port.inbound.UploadFileCommand
import org.springframework.context.annotation.Profile
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
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
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/channels/{channelId}/files")
@Profile("!test")
class SharedFileController(
    private val sharedFileUseCase: SharedFileUseCase,
    private val sharedFileQuery: SharedFileQuery,
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadFile(
        @PathVariable channelId: UUID,
        @RequestPart("file") filePart: FilePart,
        @RequestPart("senderType") senderType: String,
        @RequestPart("senderId") senderId: String,
    ): Mono<SharedFileResponse> =
        filePart
            .content()
            .map { buffer ->
                val bytes = ByteArray(buffer.readableByteCount())
                buffer.read(bytes)
                bytes
            }.reduce { acc, chunk -> acc + chunk }
            .flatMap { bytes ->
                val contentType =
                    filePart.headers().contentType?.toString() ?: MediaType.APPLICATION_OCTET_STREAM_VALUE
                sharedFileUseCase.upload(
                    UploadFileCommand(
                        channelId = channelId,
                        uploaderId = senderId,
                        uploaderType = SenderType.valueOf(senderType.uppercase()),
                        originalFilename = filePart.filename(),
                        contentType = contentType,
                        fileSize = bytes.size.toLong(),
                        content = bytes,
                    ),
                )
            }.map { it.toResponse() }

    @GetMapping
    fun listFiles(
        @PathVariable channelId: UUID,
        @RequestParam(required = false) before: Instant?,
        @RequestParam(defaultValue = "20") limit: Int,
    ): Mono<SharedFileListResponse> =
        sharedFileQuery
            .listFiles(channelId, before, limit.coerceIn(1, 100))
            .map { result ->
                SharedFileListResponse(
                    files = result.files.map { it.toResponse() },
                    hasMore = result.hasMore,
                    oldestTimestamp = result.oldestTimestamp,
                )
            }

    @GetMapping("/{fileId}/download")
    fun downloadFile(
        @PathVariable channelId: UUID,
        @PathVariable fileId: UUID,
    ): Mono<ResponseEntity<Resource>> =
        sharedFileUseCase
            .download(channelId, fileId)
            .map { fileResource ->
                val headers = HttpHeaders()
                headers.contentType = MediaType.parseMediaType(fileResource.contentType)
                headers.setContentDispositionFormData("attachment", fileResource.filename)
                headers.contentLength = fileResource.contentLength
                ResponseEntity
                    .ok()
                    .headers(headers)
                    .body(fileResource.resource)
            }

    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamFileEvents(
        @PathVariable channelId: UUID,
    ): Flux<SharedFileResponse> = sharedFileQuery.streamFileEvents(channelId).map { it.toResponse() }

    @DeleteMapping("/{fileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteFile(
        @PathVariable channelId: UUID,
        @PathVariable fileId: UUID,
    ): Mono<Void> = sharedFileUseCase.delete(channelId, fileId)

    private fun SharedFile.toResponse(): SharedFileResponse =
        SharedFileResponse(
            id = id,
            channelId = channelId,
            uploaderId = uploaderId,
            uploaderType = uploaderType.name,
            originalFilename = originalFilename,
            contentType = contentType,
            fileSize = fileSize,
            createdAt = createdAt,
        )
}
