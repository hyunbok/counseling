package com.counseling.api.application

import com.counseling.api.config.CaptureStorageProperties
import com.counseling.api.domain.ScreenCapture
import com.counseling.api.domain.exception.BadRequestException
import com.counseling.api.domain.exception.NotFoundException
import com.counseling.api.port.inbound.CaptureScreenCommand
import com.counseling.api.port.outbound.CaptureNotificationPort
import com.counseling.api.port.outbound.FileStoragePort
import com.counseling.api.port.outbound.ScreenCaptureReadRepository
import com.counseling.api.port.outbound.ScreenCaptureRepository
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

class ScreenCaptureServiceTest :
    StringSpec({
        val screenCaptureRepository = mockk<ScreenCaptureRepository>()
        val screenCaptureReadRepository = mockk<ScreenCaptureReadRepository>()
        val fileStoragePort = mockk<FileStoragePort>()
        val captureNotificationPort = mockk<CaptureNotificationPort>(relaxed = true)
        val captureStorageProperties =
            CaptureStorageProperties(
                basePath = "/tmp/test-captures",
                maxFileSize = 10_485_760L,
            )
        val screenCaptureService =
            ScreenCaptureService(
                screenCaptureRepository,
                screenCaptureReadRepository,
                fileStoragePort,
                captureNotificationPort,
                captureStorageProperties,
            )

        afterEach { clearAllMocks() }

        fun makeCapture(channelId: UUID = UUID.randomUUID()): ScreenCapture {
            val now = Instant.now()
            return ScreenCapture(
                id = UUID.randomUUID(),
                channelId = channelId,
                capturedBy = UUID.randomUUID(),
                originalFilename = "screenshot.png",
                storedFilename = "stored.png",
                contentType = "image/png",
                fileSize = 1024L,
                storagePath = "/tmp/test-captures/$channelId/stored.png",
                note = null,
                createdAt = now,
            )
        }

        fun makeCommand(
            channelId: UUID = UUID.randomUUID(),
            contentType: String = "image/png",
            content: ByteArray = byteArrayOf(1, 2, 3),
            note: String? = null,
        ): CaptureScreenCommand =
            CaptureScreenCommand(
                channelId = channelId,
                capturedBy = UUID.randomUUID(),
                originalFilename = "screenshot.png",
                contentType = contentType,
                fileSize = content.size.toLong(),
                content = content,
                note = note,
            )

        "capture should store, save, notify, and project to read repository" {
            val command = makeCommand()

            every { fileStoragePort.store(any(), any()) } returns Mono.empty()
            every { screenCaptureRepository.save(any()) } answers { Mono.just(firstArg()) }
            every { screenCaptureReadRepository.save(any()) } answers { Mono.just(firstArg()) }

            StepVerifier
                .create(screenCaptureService.capture(command))
                .assertNext { saved ->
                    saved.channelId shouldBe command.channelId
                    saved.originalFilename shouldBe "screenshot.png"
                    saved.contentType shouldBe "image/png"
                    saved.capturedBy shouldBe command.capturedBy
                }.verifyComplete()

            verify { fileStoragePort.store(any(), any()) }
            verify { screenCaptureRepository.save(any()) }
            verify { screenCaptureReadRepository.save(any()) }
            verify { captureNotificationPort.emitCapture(command.channelId, any()) }
        }

        "capture should fail with BadRequestException when content type is not PNG" {
            val command = makeCommand(contentType = "image/jpeg")

            StepVerifier
                .create(screenCaptureService.capture(command))
                .expectError(BadRequestException::class.java)
                .verify()
        }

        "capture should fail with BadRequestException when file size exceeds limit" {
            val oversizedContent = ByteArray(10_485_761) // 10MB + 1 byte
            val command = makeCommand(content = oversizedContent)

            StepVerifier
                .create(screenCaptureService.capture(command))
                .expectError(BadRequestException::class.java)
                .verify()
        }

        "capture should fail with BadRequestException when note exceeds 500 characters" {
            val longNote = "a".repeat(501)
            val command = makeCommand(note = longNote)

            StepVerifier
                .create(screenCaptureService.capture(command))
                .expectError(BadRequestException::class.java)
                .verify()
        }

        "capture should succeed even when Mongo projection fails" {
            val command = makeCommand()

            every { fileStoragePort.store(any(), any()) } returns Mono.empty()
            every { screenCaptureRepository.save(any()) } answers { Mono.just(firstArg()) }
            every { screenCaptureReadRepository.save(any()) } returns Mono.error(RuntimeException("Mongo down"))

            StepVerifier
                .create(screenCaptureService.capture(command))
                .assertNext { saved ->
                    saved.channelId shouldBe command.channelId
                }.verifyComplete()
        }

        "download should return ScreenCaptureResource from storage" {
            val channelId = UUID.randomUUID()
            val captureId = UUID.randomUUID()
            val capture = makeCapture(channelId).copy(id = captureId)
            val resource = ByteArrayResource(byteArrayOf(1, 2, 3))

            every { screenCaptureRepository.findByIdAndNotDeleted(captureId) } returns Mono.just(capture)
            every { fileStoragePort.load(capture.storagePath) } returns Mono.just(resource)

            StepVerifier
                .create(screenCaptureService.download(channelId, captureId))
                .assertNext { result ->
                    result.filename shouldBe capture.originalFilename
                    result.contentType shouldBe capture.contentType
                    result.contentLength shouldBe capture.fileSize
                }.verifyComplete()
        }

        "download should fail with NotFoundException when capture does not exist" {
            val channelId = UUID.randomUUID()
            val captureId = UUID.randomUUID()

            every { screenCaptureRepository.findByIdAndNotDeleted(captureId) } returns Mono.empty()

            StepVerifier
                .create(screenCaptureService.download(channelId, captureId))
                .expectError(NotFoundException::class.java)
                .verify()
        }

        "delete should soft-delete capture and mark deleted in read store" {
            val channelId = UUID.randomUUID()
            val captureId = UUID.randomUUID()
            val capture = makeCapture(channelId).copy(id = captureId)

            every { screenCaptureRepository.findByIdAndNotDeleted(captureId) } returns Mono.just(capture)
            every { screenCaptureRepository.softDelete(captureId) } returns Mono.empty()
            every { screenCaptureReadRepository.markDeleted(captureId) } returns Mono.empty()
            every { fileStoragePort.delete(capture.storagePath) } returns Mono.empty()

            StepVerifier
                .create(screenCaptureService.delete(channelId, captureId))
                .verifyComplete()

            verify { screenCaptureRepository.softDelete(captureId) }
            verify { screenCaptureReadRepository.markDeleted(captureId) }
            verify { fileStoragePort.delete(capture.storagePath) }
        }

        "delete should fail with NotFoundException when capture does not exist" {
            val channelId = UUID.randomUUID()
            val captureId = UUID.randomUUID()

            every { screenCaptureRepository.findByIdAndNotDeleted(captureId) } returns Mono.empty()

            StepVerifier
                .create(screenCaptureService.delete(channelId, captureId))
                .expectError(NotFoundException::class.java)
                .verify()
        }
    })
