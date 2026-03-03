package com.counseling.api.application

import com.counseling.api.config.CaptureStorageProperties
import com.counseling.api.domain.ScreenCapture
import com.counseling.api.domain.exception.BadRequestException
import com.counseling.api.domain.exception.NotFoundException
import com.counseling.api.port.inbound.CaptureScreenCommand
import com.counseling.api.port.inbound.ScreenCaptureResource
import com.counseling.api.port.inbound.ScreenCaptureUseCase
import com.counseling.api.port.outbound.CaptureNotificationPort
import com.counseling.api.port.outbound.FileStoragePort
import com.counseling.api.port.outbound.ScreenCaptureReadRepository
import com.counseling.api.port.outbound.ScreenCaptureRepository
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Service
@Profile("!test")
class ScreenCaptureService(
    private val screenCaptureRepository: ScreenCaptureRepository,
    private val screenCaptureReadRepository: ScreenCaptureReadRepository,
    private val fileStoragePort: FileStoragePort,
    private val captureNotificationPort: CaptureNotificationPort,
    private val captureStorageProperties: CaptureStorageProperties,
) : ScreenCaptureUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun capture(command: CaptureScreenCommand): Mono<ScreenCapture> {
        if (command.contentType != "image/png") {
            return Mono.error(BadRequestException("Only PNG captures are allowed: ${command.contentType}"))
        }
        val actualSize = command.content.size.toLong()
        if (actualSize > captureStorageProperties.maxFileSize) {
            return Mono.error(
                BadRequestException("File size $actualSize exceeds limit ${captureStorageProperties.maxFileSize}"),
            )
        }
        if (command.note != null && command.note.length > 500) {
            return Mono.error(BadRequestException("Note exceeds maximum length of 500 characters"))
        }
        if (command.content.size < 8 || !command.content.copyOfRange(0, 8).contentEquals(PNG_MAGIC)) {
            return Mono.error(BadRequestException("Content is not a valid PNG file"))
        }

        val storedFilename = "${UUID.randomUUID()}.png"
        val storagePath = "${captureStorageProperties.basePath}/${command.channelId}/$storedFilename"

        val capture =
            ScreenCapture(
                id = UUID.randomUUID(),
                channelId = command.channelId,
                capturedBy = command.capturedBy,
                originalFilename = command.originalFilename,
                storedFilename = storedFilename,
                contentType = command.contentType,
                fileSize = actualSize,
                storagePath = storagePath,
                note = command.note,
                createdAt = Instant.now(),
            )

        return fileStoragePort
            .store(storagePath, command.content)
            .then(screenCaptureRepository.save(capture))
            .onErrorResume { e ->
                fileStoragePort
                    .delete(storagePath)
                    .doOnError { deleteError ->
                        log.error(
                            "Failed to rollback file at {} after DB save failure: {}",
                            storagePath,
                            deleteError.message,
                        )
                    }.onErrorComplete()
                    .then(Mono.error(e))
            }.doOnNext { saved ->
                captureNotificationPort.emitCapture(command.channelId, saved)
            }.flatMap { saved ->
                screenCaptureReadRepository
                    .save(saved)
                    .thenReturn(saved)
                    .onErrorResume { e ->
                        log.error(
                            "Failed to project screen capture {} to read store: {}",
                            saved.id,
                            e.message,
                        )
                        Mono.just(saved)
                    }
            }
    }

    override fun download(
        channelId: UUID,
        captureId: UUID,
    ): Mono<ScreenCaptureResource> =
        screenCaptureRepository
            .findByIdAndNotDeleted(captureId)
            .switchIfEmpty(Mono.error(NotFoundException("Screen capture not found: $captureId")))
            .flatMap { capture ->
                if (capture.channelId != channelId) {
                    return@flatMap Mono.error(NotFoundException("Screen capture not found: $captureId"))
                }
                fileStoragePort.load(capture.storagePath).map { resource ->
                    ScreenCaptureResource(
                        resource = resource,
                        filename = capture.originalFilename,
                        contentType = capture.contentType,
                        contentLength = capture.fileSize,
                    )
                }
            }

    override fun delete(
        channelId: UUID,
        captureId: UUID,
    ): Mono<Void> =
        screenCaptureRepository
            .findByIdAndNotDeleted(captureId)
            .switchIfEmpty(Mono.error(NotFoundException("Screen capture not found: $captureId")))
            .flatMap { capture ->
                if (capture.channelId != channelId) {
                    return@flatMap Mono.error(NotFoundException("Screen capture not found: $captureId"))
                }
                screenCaptureRepository
                    .softDelete(captureId)
                    .then(
                        screenCaptureReadRepository
                            .markDeleted(captureId)
                            .onErrorResume { e ->
                                log.error(
                                    "Failed to mark capture {} as deleted in read store: {}",
                                    captureId,
                                    e.message,
                                )
                                Mono.empty()
                            },
                    ).then(fileStoragePort.delete(capture.storagePath))
            }

    companion object {
        private val PNG_MAGIC =
            byteArrayOf(
                0x89.toByte(),
                0x50,
                0x4E,
                0x47,
                0x0D,
                0x0A,
                0x1A,
                0x0A,
            )
    }
}
