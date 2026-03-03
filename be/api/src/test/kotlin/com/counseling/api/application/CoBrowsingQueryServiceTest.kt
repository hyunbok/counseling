package com.counseling.api.application

import com.counseling.api.domain.CoBrowsingSession
import com.counseling.api.domain.CoBrowsingStatus
import com.counseling.api.port.outbound.CoBrowsingNotificationPort
import com.counseling.api.port.outbound.CoBrowsingSessionReadRepository
import com.counseling.api.port.outbound.CoBrowsingSessionRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.util.UUID

class CoBrowsingQueryServiceTest :
    StringSpec({
        val coBrowsingSessionRepository = mockk<CoBrowsingSessionRepository>()
        val coBrowsingSessionReadRepository = mockk<CoBrowsingSessionReadRepository>()
        val coBrowsingNotificationPort = mockk<CoBrowsingNotificationPort>(relaxed = true)
        val coBrowsingQueryService =
            CoBrowsingQueryService(
                coBrowsingSessionRepository,
                coBrowsingSessionReadRepository,
                coBrowsingNotificationPort,
            )

        afterEach { clearAllMocks() }

        fun makeSession(
            channelId: UUID,
            status: CoBrowsingStatus = CoBrowsingStatus.ACTIVE,
            createdAt: Instant = Instant.now(),
        ): CoBrowsingSession {
            val now = Instant.now()
            return CoBrowsingSession(
                id = UUID.randomUUID(),
                channelId = channelId,
                initiatedBy = UUID.randomUUID(),
                status = status,
                startedAt = if (status == CoBrowsingStatus.ACTIVE || status == CoBrowsingStatus.ENDED) now else null,
                endedAt = if (status == CoBrowsingStatus.ENDED) now else null,
                createdAt = createdAt,
                updatedAt = now,
            )
        }

        "getActiveSession should return active session from repository" {
            val channelId = UUID.randomUUID()
            val session = makeSession(channelId, CoBrowsingStatus.ACTIVE)

            every { coBrowsingSessionRepository.findActiveByChannelId(channelId) } returns Mono.just(session)

            StepVerifier
                .create(coBrowsingQueryService.getActiveSession(channelId))
                .assertNext { found ->
                    found.id shouldBe session.id
                    found.channelId shouldBe channelId
                    found.status shouldBe CoBrowsingStatus.ACTIVE
                }.verifyComplete()

            verify { coBrowsingSessionRepository.findActiveByChannelId(channelId) }
        }

        "getActiveSession should return empty when no active session" {
            val channelId = UUID.randomUUID()

            every { coBrowsingSessionRepository.findActiveByChannelId(channelId) } returns Mono.empty()

            StepVerifier
                .create(coBrowsingQueryService.getActiveSession(channelId))
                .verifyComplete()
        }

        "listSessions should return sessions with hasMore false when within limit" {
            val channelId = UUID.randomUUID()
            val sessions = (1..3).map { makeSession(channelId, CoBrowsingStatus.ENDED) }

            every { coBrowsingSessionReadRepository.findByChannelId(channelId, null, 4) } returns
                Flux.fromIterable(sessions)

            StepVerifier
                .create(coBrowsingQueryService.listSessions(channelId, null, 3))
                .assertNext { result ->
                    result.sessions.size shouldBe 3
                    result.hasMore shouldBe false
                }.verifyComplete()
        }

        "listSessions should return hasMore true and trim last item when result exceeds limit" {
            val channelId = UUID.randomUUID()
            val sessions = (1..4).map { makeSession(channelId, CoBrowsingStatus.ENDED) }

            every { coBrowsingSessionReadRepository.findByChannelId(channelId, null, 4) } returns
                Flux.fromIterable(sessions)

            StepVerifier
                .create(coBrowsingQueryService.listSessions(channelId, null, 3))
                .assertNext { result ->
                    result.sessions.size shouldBe 3
                    result.hasMore shouldBe true
                }.verifyComplete()
        }

        "listSessions should pass before cursor to read repository" {
            val channelId = UUID.randomUUID()
            val before = Instant.now().minusSeconds(60)
            val sessions = (1..2).map { makeSession(channelId, CoBrowsingStatus.ENDED) }

            every { coBrowsingSessionReadRepository.findByChannelId(channelId, before, 11) } returns
                Flux.fromIterable(sessions)

            StepVerifier
                .create(coBrowsingQueryService.listSessions(channelId, before, 10))
                .assertNext { result ->
                    result.sessions.size shouldBe 2
                    result.hasMore shouldBe false
                }.verifyComplete()

            verify { coBrowsingSessionReadRepository.findByChannelId(channelId, before, 11) }
        }

        "listSessions should set oldestTimestamp from the first session in reversed list" {
            val channelId = UUID.randomUUID()
            val older = Instant.now().minusSeconds(120)
            val newer = Instant.now().minusSeconds(60)
            val sessions =
                listOf(
                    makeSession(channelId, CoBrowsingStatus.ENDED, createdAt = newer),
                    makeSession(channelId, CoBrowsingStatus.ENDED, createdAt = older),
                )

            every { coBrowsingSessionReadRepository.findByChannelId(channelId, null, 3) } returns
                Flux.fromIterable(sessions)

            StepVerifier
                .create(coBrowsingQueryService.listSessions(channelId, null, 2))
                .assertNext { result ->
                    result.oldestTimestamp shouldBe older
                }.verifyComplete()
        }

        "listSessions should return empty result when no sessions found" {
            val channelId = UUID.randomUUID()

            every { coBrowsingSessionReadRepository.findByChannelId(channelId, null, 11) } returns Flux.empty()

            StepVerifier
                .create(coBrowsingQueryService.listSessions(channelId, null, 10))
                .assertNext { result ->
                    result.sessions.size shouldBe 0
                    result.hasMore shouldBe false
                    result.oldestTimestamp shouldBe null
                }.verifyComplete()
        }

        "streamUpdates should delegate to coBrowsingNotificationPort" {
            val channelId = UUID.randomUUID()
            val session = makeSession(channelId)

            every { coBrowsingNotificationPort.subscribeSessionUpdates(channelId) } returns Flux.just(session)

            StepVerifier
                .create(coBrowsingQueryService.streamUpdates(channelId))
                .assertNext { received ->
                    received.id shouldBe session.id
                    received.channelId shouldBe channelId
                }.verifyComplete()

            verify { coBrowsingNotificationPort.subscribeSessionUpdates(channelId) }
        }
    })
