package com.counseling.api.application

import com.counseling.api.domain.ScreenCapture
import com.counseling.api.port.outbound.CaptureNotificationPort
import com.counseling.api.port.outbound.ScreenCaptureReadRepository
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

class ScreenCaptureQueryServiceTest :
    StringSpec({
        val screenCaptureReadRepository = mockk<ScreenCaptureReadRepository>()
        val captureNotificationPort = mockk<CaptureNotificationPort>(relaxed = true)
        val screenCaptureQueryService =
            ScreenCaptureQueryService(
                screenCaptureReadRepository,
                captureNotificationPort,
            )

        afterEach { clearAllMocks() }

        fun makeCapture(
            channelId: UUID,
            createdAt: Instant = Instant.now(),
        ): ScreenCapture =
            ScreenCapture(
                id = UUID.randomUUID(),
                channelId = channelId,
                capturedBy = UUID.randomUUID(),
                originalFilename = "screenshot.png",
                storedFilename = "stored.png",
                contentType = "image/png",
                fileSize = 1024L,
                storagePath = "/tmp/test-captures/$channelId/stored.png",
                note = null,
                createdAt = createdAt,
            )

        "listCaptures should return captures from read repository with hasMore false when within limit" {
            val channelId = UUID.randomUUID()
            val captures = (1..3).map { makeCapture(channelId) }

            every { screenCaptureReadRepository.findByChannelId(channelId, null, 4) } returns
                Flux.fromIterable(captures)

            StepVerifier
                .create(screenCaptureQueryService.listCaptures(channelId, null, 3))
                .assertNext { result ->
                    result.captures.size shouldBe 3
                    result.hasMore shouldBe false
                }.verifyComplete()
        }

        "listCaptures should return hasMore true and trim last item when result exceeds limit" {
            val channelId = UUID.randomUUID()
            val captures = (1..4).map { makeCapture(channelId) }

            every { screenCaptureReadRepository.findByChannelId(channelId, null, 4) } returns
                Flux.fromIterable(captures)

            StepVerifier
                .create(screenCaptureQueryService.listCaptures(channelId, null, 3))
                .assertNext { result ->
                    result.captures.size shouldBe 3
                    result.hasMore shouldBe true
                }.verifyComplete()
        }

        "listCaptures should pass before parameter to read repository for cursor pagination" {
            val channelId = UUID.randomUUID()
            val before = Instant.now().minusSeconds(60)
            val captures = (1..2).map { makeCapture(channelId) }

            every { screenCaptureReadRepository.findByChannelId(channelId, before, 11) } returns
                Flux.fromIterable(captures)

            StepVerifier
                .create(screenCaptureQueryService.listCaptures(channelId, before, 10))
                .assertNext { result ->
                    result.captures.size shouldBe 2
                    result.hasMore shouldBe false
                }.verifyComplete()

            verify { screenCaptureReadRepository.findByChannelId(channelId, before, 11) }
        }

        "listCaptures should set oldestTimestamp from the first capture in reversed list" {
            val channelId = UUID.randomUUID()
            val older = Instant.now().minusSeconds(120)
            val newer = Instant.now().minusSeconds(60)
            val captures =
                listOf(
                    makeCapture(channelId, createdAt = newer),
                    makeCapture(channelId, createdAt = older),
                )

            every { screenCaptureReadRepository.findByChannelId(channelId, null, 3) } returns
                Flux.fromIterable(captures)

            StepVerifier
                .create(screenCaptureQueryService.listCaptures(channelId, null, 2))
                .assertNext { result ->
                    result.oldestTimestamp shouldBe older
                }.verifyComplete()
        }

        "listCaptures should return empty result when no captures found" {
            val channelId = UUID.randomUUID()

            every { screenCaptureReadRepository.findByChannelId(channelId, null, 11) } returns Flux.empty()

            StepVerifier
                .create(screenCaptureQueryService.listCaptures(channelId, null, 10))
                .assertNext { result ->
                    result.captures.size shouldBe 0
                    result.hasMore shouldBe false
                    result.oldestTimestamp shouldBe null
                }.verifyComplete()
        }

        "streamCaptureEvents should delegate to captureNotificationPort" {
            val channelId = UUID.randomUUID()
            val capture = makeCapture(channelId)

            every { captureNotificationPort.subscribeCaptures(channelId) } returns Flux.just(capture)

            StepVerifier
                .create(screenCaptureQueryService.streamCaptureEvents(channelId))
                .assertNext { received ->
                    received.id shouldBe capture.id
                    received.channelId shouldBe channelId
                }.verifyComplete()

            verify { captureNotificationPort.subscribeCaptures(channelId) }
        }
    })
