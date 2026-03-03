package com.counseling.api.application

import com.counseling.api.domain.SenderType
import com.counseling.api.domain.SharedFile
import com.counseling.api.port.outbound.FileNotificationPort
import com.counseling.api.port.outbound.SharedFileReadRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import java.time.Instant
import java.util.UUID

class SharedFileQueryServiceTest :
    StringSpec({
        val sharedFileReadRepository = mockk<SharedFileReadRepository>()
        val fileNotificationPort = mockk<FileNotificationPort>(relaxed = true)
        val sharedFileQueryService =
            SharedFileQueryService(
                sharedFileReadRepository,
                fileNotificationPort,
            )

        afterEach { clearAllMocks() }

        fun makeFile(
            channelId: UUID,
            createdAt: Instant = Instant.now(),
        ): SharedFile =
            SharedFile(
                id = UUID.randomUUID(),
                channelId = channelId,
                uploaderId = "agent-1",
                uploaderType = SenderType.AGENT,
                originalFilename = "doc.pdf",
                storedFilename = "stored.pdf",
                contentType = "application/pdf",
                fileSize = 2048L,
                storagePath = "/tmp/test-files/$channelId/stored.pdf",
                createdAt = createdAt,
            )

        "listFiles should return files from read repository with hasMore false when within limit" {
            val channelId = UUID.randomUUID()
            val files = (1..3).map { makeFile(channelId) }

            every { sharedFileReadRepository.findByChannelId(channelId, null, 4) } returns
                Flux.fromIterable(files)

            StepVerifier
                .create(sharedFileQueryService.listFiles(channelId, null, 3))
                .assertNext { result ->
                    result.files.size shouldBe 3
                    result.hasMore shouldBe false
                }.verifyComplete()
        }

        "listFiles should return hasMore true and trim last item when result exceeds limit" {
            val channelId = UUID.randomUUID()
            val files = (1..4).map { makeFile(channelId) }

            every { sharedFileReadRepository.findByChannelId(channelId, null, 4) } returns
                Flux.fromIterable(files)

            StepVerifier
                .create(sharedFileQueryService.listFiles(channelId, null, 3))
                .assertNext { result ->
                    result.files.size shouldBe 3
                    result.hasMore shouldBe true
                }.verifyComplete()
        }

        "listFiles should pass before parameter to read repository for cursor pagination" {
            val channelId = UUID.randomUUID()
            val before = Instant.now().minusSeconds(60)
            val files = (1..2).map { makeFile(channelId) }

            every { sharedFileReadRepository.findByChannelId(channelId, before, 11) } returns
                Flux.fromIterable(files)

            StepVerifier
                .create(sharedFileQueryService.listFiles(channelId, before, 10))
                .assertNext { result ->
                    result.files.size shouldBe 2
                    result.hasMore shouldBe false
                }.verifyComplete()

            verify { sharedFileReadRepository.findByChannelId(channelId, before, 11) }
        }

        "listFiles should set oldestTimestamp from the first file in reversed list" {
            val channelId = UUID.randomUUID()
            val older = Instant.now().minusSeconds(120)
            val newer = Instant.now().minusSeconds(60)
            val files =
                listOf(
                    makeFile(channelId, createdAt = newer),
                    makeFile(channelId, createdAt = older),
                )

            every { sharedFileReadRepository.findByChannelId(channelId, null, 3) } returns
                Flux.fromIterable(files)

            StepVerifier
                .create(sharedFileQueryService.listFiles(channelId, null, 2))
                .assertNext { result ->
                    result.oldestTimestamp shouldBe older
                }.verifyComplete()
        }

        "listFiles should return empty result when no files found" {
            val channelId = UUID.randomUUID()

            every { sharedFileReadRepository.findByChannelId(channelId, null, 11) } returns Flux.empty()

            StepVerifier
                .create(sharedFileQueryService.listFiles(channelId, null, 10))
                .assertNext { result ->
                    result.files.size shouldBe 0
                    result.hasMore shouldBe false
                    result.oldestTimestamp shouldBe null
                }.verifyComplete()
        }

        "streamFileEvents should delegate to fileNotificationPort" {
            val channelId = UUID.randomUUID()
            val file = makeFile(channelId)

            every { fileNotificationPort.subscribeFiles(channelId) } returns Flux.just(file)

            StepVerifier
                .create(sharedFileQueryService.streamFileEvents(channelId))
                .assertNext { received ->
                    received.id shouldBe file.id
                    received.channelId shouldBe channelId
                }.verifyComplete()

            verify { fileNotificationPort.subscribeFiles(channelId) }
        }
    })
