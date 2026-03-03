package com.counseling.api.application

import com.counseling.api.config.FileStorageProperties
import com.counseling.api.domain.SenderType
import com.counseling.api.domain.SharedFile
import com.counseling.api.domain.exception.BadRequestException
import com.counseling.api.domain.exception.NotFoundException
import com.counseling.api.port.inbound.UploadFileCommand
import com.counseling.api.port.outbound.FileNotificationPort
import com.counseling.api.port.outbound.FileStoragePort
import com.counseling.api.port.outbound.SharedFileReadRepository
import com.counseling.api.port.outbound.SharedFileRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.core.io.ByteArrayResource
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.util.UUID

class SharedFileServiceTest :
    StringSpec({
        val sharedFileRepository = mockk<SharedFileRepository>()
        val sharedFileReadRepository = mockk<SharedFileReadRepository>()
        val fileStoragePort = mockk<FileStoragePort>()
        val fileNotificationPort = mockk<FileNotificationPort>(relaxed = true)
        val fileStorageProperties =
            FileStorageProperties(
                basePath = "/tmp/test-files",
                maxFileSize = 10_485_760L,
                allowedTypes = listOf("image/jpeg", "image/png", "application/pdf"),
            )
        val sharedFileService =
            SharedFileService(
                sharedFileRepository,
                sharedFileReadRepository,
                fileStoragePort,
                fileNotificationPort,
                fileStorageProperties,
            )

        afterEach { clearAllMocks() }

        fun makeFile(channelId: UUID = UUID.randomUUID()): SharedFile {
            val now = Instant.now()
            return SharedFile(
                id = UUID.randomUUID(),
                channelId = channelId,
                uploaderId = "agent-1",
                uploaderType = SenderType.AGENT,
                originalFilename = "test.png",
                storedFilename = "stored.png",
                contentType = "image/png",
                fileSize = 1024L,
                storagePath = "/tmp/test-files/$channelId/stored.png",
                createdAt = now,
            )
        }

        fun makeCommand(
            channelId: UUID = UUID.randomUUID(),
            contentType: String = "image/png",
            fileSize: Long = 1024L,
            content: ByteArray = byteArrayOf(1, 2, 3),
        ): UploadFileCommand =
            UploadFileCommand(
                channelId = channelId,
                uploaderId = "agent-1",
                uploaderType = SenderType.AGENT,
                originalFilename = "test.png",
                contentType = contentType,
                fileSize = fileSize,
                content = content,
            )

        "upload should store, save, notify, and project to read repository" {
            val command = makeCommand()

            every { fileStoragePort.store(any(), any()) } returns Mono.empty()
            every { sharedFileRepository.save(any()) } answers { Mono.just(firstArg()) }
            every { sharedFileReadRepository.save(any()) } answers { Mono.just(firstArg()) }

            StepVerifier
                .create(sharedFileService.upload(command))
                .assertNext { saved ->
                    saved.channelId shouldBe command.channelId
                    saved.originalFilename shouldBe "test.png"
                    saved.contentType shouldBe "image/png"
                    saved.uploaderId shouldBe "agent-1"
                }.verifyComplete()

            verify { fileStoragePort.store(any(), any()) }
            verify { sharedFileRepository.save(any()) }
            verify { sharedFileReadRepository.save(any()) }
            verify { fileNotificationPort.emitFile(command.channelId, any()) }
        }

        "upload should fail with BadRequestException when content type is not allowed" {
            val command = makeCommand(contentType = "text/html")

            StepVerifier
                .create(sharedFileService.upload(command))
                .expectError(BadRequestException::class.java)
                .verify()
        }

        "upload should fail with BadRequestException when file size exceeds limit" {
            val oversizedContent = ByteArray(10_485_761) // 10MB + 1 byte
            val command =
                makeCommand(
                    fileSize = 10_485_761L,
                    content = oversizedContent,
                )

            StepVerifier
                .create(sharedFileService.upload(command))
                .expectError(BadRequestException::class.java)
                .verify()
        }

        "upload should succeed even when Mongo projection fails" {
            val command = makeCommand()

            every { fileStoragePort.store(any(), any()) } returns Mono.empty()
            every { sharedFileRepository.save(any()) } answers { Mono.just(firstArg()) }
            every { sharedFileReadRepository.save(any()) } returns Mono.error(RuntimeException("Mongo down"))

            StepVerifier
                .create(sharedFileService.upload(command))
                .assertNext { saved ->
                    saved.channelId shouldBe command.channelId
                }.verifyComplete()
        }

        "download should return SharedFileResource from storage" {
            val channelId = UUID.randomUUID()
            val fileId = UUID.randomUUID()
            val file = makeFile(channelId).copy(id = fileId)
            val resource = ByteArrayResource(byteArrayOf(1, 2, 3))

            every { sharedFileRepository.findByIdAndNotDeleted(fileId) } returns Mono.just(file)
            every { fileStoragePort.load(file.storagePath) } returns Mono.just(resource)

            StepVerifier
                .create(sharedFileService.download(channelId, fileId))
                .assertNext { result ->
                    result.filename shouldBe file.originalFilename
                    result.contentType shouldBe file.contentType
                    result.contentLength shouldBe file.fileSize
                }.verifyComplete()
        }

        "download should fail with NotFoundException when file does not exist" {
            val channelId = UUID.randomUUID()
            val fileId = UUID.randomUUID()

            every { sharedFileRepository.findByIdAndNotDeleted(fileId) } returns Mono.empty()

            StepVerifier
                .create(sharedFileService.download(channelId, fileId))
                .expectError(NotFoundException::class.java)
                .verify()
        }

        "delete should soft-delete file and mark deleted in read store" {
            val channelId = UUID.randomUUID()
            val fileId = UUID.randomUUID()
            val file = makeFile(channelId).copy(id = fileId)

            every { sharedFileRepository.findByIdAndNotDeleted(fileId) } returns Mono.just(file)
            every { sharedFileRepository.softDelete(fileId) } returns Mono.empty()
            every { sharedFileReadRepository.markDeleted(fileId) } returns Mono.empty()
            every { fileStoragePort.delete(file.storagePath) } returns Mono.empty()

            StepVerifier
                .create(sharedFileService.delete(channelId, fileId))
                .verifyComplete()

            verify { sharedFileRepository.softDelete(fileId) }
            verify { sharedFileReadRepository.markDeleted(fileId) }
            verify { fileStoragePort.delete(file.storagePath) }
        }

        "delete should fail with NotFoundException when file does not exist" {
            val channelId = UUID.randomUUID()
            val fileId = UUID.randomUUID()

            every { sharedFileRepository.findByIdAndNotDeleted(fileId) } returns Mono.empty()

            StepVerifier
                .create(sharedFileService.delete(channelId, fileId))
                .expectError(NotFoundException::class.java)
                .verify()
        }
    })
