package com.counseling.api.application

import com.counseling.api.config.FileStorageProperties
import com.counseling.api.domain.SharedFile
import com.counseling.api.domain.exception.BadRequestException
import com.counseling.api.domain.exception.NotFoundException
import com.counseling.api.port.inbound.SharedFileResource
import com.counseling.api.port.inbound.SharedFileUseCase
import com.counseling.api.port.inbound.UploadFileCommand
import com.counseling.api.port.outbound.FileNotificationPort
import com.counseling.api.port.outbound.FileStoragePort
import com.counseling.api.port.outbound.SharedFileReadRepository
import com.counseling.api.port.outbound.SharedFileRepository
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Service
@Profile("!test")
class SharedFileService(
    private val sharedFileRepository: SharedFileRepository,
    private val sharedFileReadRepository: SharedFileReadRepository,
    private val fileStoragePort: FileStoragePort,
    private val fileNotificationPort: FileNotificationPort,
    private val fileStorageProperties: FileStorageProperties,
) : SharedFileUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun upload(command: UploadFileCommand): Mono<SharedFile> {
        if (command.contentType !in fileStorageProperties.allowedTypes) {
            return Mono.error(BadRequestException("File type not allowed: ${command.contentType}"))
        }
        val actualSize = command.content.size.toLong()
        if (actualSize > fileStorageProperties.maxFileSize) {
            return Mono.error(
                BadRequestException("File size $actualSize exceeds limit ${fileStorageProperties.maxFileSize}"),
            )
        }

        val rawExtension = command.originalFilename.substringAfterLast('.', "")
        val safeExtension = rawExtension.replace(Regex("[^a-zA-Z0-9]"), "").take(10)
        val storedFilename =
            if (safeExtension.isNotBlank()) "${UUID.randomUUID()}.$safeExtension" else UUID.randomUUID().toString()
        val storagePath = "${fileStorageProperties.basePath}/${command.channelId}/$storedFilename"

        val file =
            SharedFile(
                id = UUID.randomUUID(),
                channelId = command.channelId,
                uploaderId = command.uploaderId,
                uploaderType = command.uploaderType,
                originalFilename = command.originalFilename,
                storedFilename = storedFilename,
                contentType = command.contentType,
                fileSize = actualSize,
                storagePath = storagePath,
                createdAt = Instant.now(),
            )

        return fileStoragePort
            .store(storagePath, command.content)
            .then(sharedFileRepository.save(file))
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
                fileNotificationPort.emitFile(command.channelId, saved)
            }.flatMap { saved ->
                sharedFileReadRepository
                    .save(saved)
                    .thenReturn(saved)
                    .onErrorResume { e ->
                        log.error(
                            "Failed to project shared file {} to read store: {}",
                            saved.id,
                            e.message,
                        )
                        Mono.just(saved)
                    }
            }
    }

    override fun download(
        channelId: UUID,
        fileId: UUID,
    ): Mono<SharedFileResource> =
        sharedFileRepository
            .findByIdAndNotDeleted(fileId)
            .switchIfEmpty(Mono.error(NotFoundException("File not found: $fileId")))
            .flatMap { file ->
                if (file.channelId != channelId) {
                    return@flatMap Mono.error(NotFoundException("File not found: $fileId"))
                }
                fileStoragePort.load(file.storagePath).map { resource ->
                    SharedFileResource(
                        resource = resource,
                        filename = file.originalFilename,
                        contentType = file.contentType,
                        contentLength = file.fileSize,
                    )
                }
            }

    override fun delete(
        channelId: UUID,
        fileId: UUID,
    ): Mono<Void> =
        sharedFileRepository
            .findByIdAndNotDeleted(fileId)
            .switchIfEmpty(Mono.error(NotFoundException("File not found: $fileId")))
            .flatMap { file ->
                if (file.channelId != channelId) {
                    return@flatMap Mono.error(NotFoundException("File not found: $fileId"))
                }
                sharedFileRepository
                    .softDelete(fileId)
                    .then(
                        sharedFileReadRepository
                            .markDeleted(fileId)
                            .onErrorResume { e ->
                                log.error("Failed to mark file {} as deleted in read store: {}", fileId, e.message)
                                Mono.empty()
                            },
                    ).then(fileStoragePort.delete(file.storagePath))
            }
}
