package com.counseling.api.application

import com.counseling.api.domain.Channel
import com.counseling.api.domain.ChannelStatus
import com.counseling.api.domain.CoBrowsingSession
import com.counseling.api.domain.CoBrowsingStatus
import com.counseling.api.domain.exception.ConflictException
import com.counseling.api.domain.exception.NotFoundException
import com.counseling.api.port.inbound.EndCoBrowsingCommand
import com.counseling.api.port.inbound.RequestCoBrowsingCommand
import com.counseling.api.port.inbound.StartCoBrowsingCommand
import com.counseling.api.port.outbound.ChannelRepository
import com.counseling.api.port.outbound.CoBrowsingNotificationPort
import com.counseling.api.port.outbound.CoBrowsingSessionReadRepository
import com.counseling.api.port.outbound.CoBrowsingSessionRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.util.UUID

class CoBrowsingServiceTest :
    StringSpec({
        val channelRepository = mockk<ChannelRepository>()
        val coBrowsingSessionRepository = mockk<CoBrowsingSessionRepository>()
        val coBrowsingSessionReadRepository = mockk<CoBrowsingSessionReadRepository>()
        val coBrowsingNotificationPort = mockk<CoBrowsingNotificationPort>(relaxed = true)
        val coBrowsingService =
            CoBrowsingService(
                channelRepository,
                coBrowsingSessionRepository,
                coBrowsingSessionReadRepository,
                coBrowsingNotificationPort,
            )

        afterEach { clearAllMocks() }

        fun makeChannel(status: ChannelStatus = ChannelStatus.IN_PROGRESS): Channel {
            val now = Instant.now()
            return Channel(
                id = UUID.randomUUID(),
                agentId = UUID.randomUUID(),
                status = status,
                startedAt = now,
                endedAt = null,
                recordingPath = null,
                livekitRoomName = "room-1",
                createdAt = now,
                updatedAt = now,
            )
        }

        fun makeSession(
            channelId: UUID,
            status: CoBrowsingStatus = CoBrowsingStatus.REQUESTED,
        ): CoBrowsingSession {
            val now = Instant.now()
            return CoBrowsingSession(
                id = UUID.randomUUID(),
                channelId = channelId,
                initiatedBy = UUID.randomUUID(),
                status = status,
                startedAt = if (status == CoBrowsingStatus.ACTIVE || status == CoBrowsingStatus.ENDED) now else null,
                endedAt = if (status == CoBrowsingStatus.ENDED) now else null,
                createdAt = now,
                updatedAt = now,
            )
        }

        "requestSession should create REQUESTED session and emit SSE when channel is IN_PROGRESS" {
            val channel = makeChannel(ChannelStatus.IN_PROGRESS)
            val agentId = UUID.randomUUID()
            val command = RequestCoBrowsingCommand(channelId = channel.id, agentId = agentId)

            every { channelRepository.findByIdAndNotDeleted(channel.id) } returns Mono.just(channel)
            every { coBrowsingSessionRepository.findActiveByChannelId(channel.id) } returns Mono.empty()
            every { coBrowsingSessionRepository.save(any()) } answers { Mono.just(firstArg()) }
            every { coBrowsingSessionReadRepository.save(any()) } answers { Mono.just(firstArg()) }

            StepVerifier
                .create(coBrowsingService.requestSession(command))
                .assertNext { saved ->
                    saved.channelId shouldBe channel.id
                    saved.initiatedBy shouldBe agentId
                    saved.status shouldBe CoBrowsingStatus.REQUESTED
                    saved.startedAt shouldBe null
                    saved.endedAt shouldBe null
                }.verifyComplete()

            verify { coBrowsingSessionRepository.save(any()) }
            verify { coBrowsingSessionReadRepository.save(any()) }
            verify { coBrowsingNotificationPort.emitSessionUpdate(channel.id, any()) }
        }

        "requestSession should throw NotFoundException when channel not found" {
            val channelId = UUID.randomUUID()
            val command = RequestCoBrowsingCommand(channelId = channelId, agentId = UUID.randomUUID())

            every { channelRepository.findByIdAndNotDeleted(channelId) } returns Mono.empty()

            StepVerifier
                .create(coBrowsingService.requestSession(command))
                .expectError(NotFoundException::class.java)
                .verify()
        }

        "requestSession should throw ConflictException when channel is not IN_PROGRESS" {
            val channel = makeChannel(ChannelStatus.CLOSED)
            val command = RequestCoBrowsingCommand(channelId = channel.id, agentId = UUID.randomUUID())

            every { channelRepository.findByIdAndNotDeleted(channel.id) } returns Mono.just(channel)

            StepVerifier
                .create(coBrowsingService.requestSession(command))
                .expectError(ConflictException::class.java)
                .verify()
        }

        "requestSession should throw ConflictException when session already active" {
            val channel = makeChannel(ChannelStatus.IN_PROGRESS)
            val activeSession = makeSession(channel.id, CoBrowsingStatus.ACTIVE)
            val command = RequestCoBrowsingCommand(channelId = channel.id, agentId = UUID.randomUUID())

            every { channelRepository.findByIdAndNotDeleted(channel.id) } returns Mono.just(channel)
            every { coBrowsingSessionRepository.findActiveByChannelId(channel.id) } returns Mono.just(activeSession)

            StepVerifier
                .create(coBrowsingService.requestSession(command))
                .expectError(ConflictException::class.java)
                .verify()
        }

        "startSession should transition session to ACTIVE" {
            val channelId = UUID.randomUUID()
            val session = makeSession(channelId, CoBrowsingStatus.REQUESTED)
            val command = StartCoBrowsingCommand(channelId = channelId, sessionId = session.id)

            every { coBrowsingSessionRepository.findByIdAndNotDeleted(session.id) } returns Mono.just(session)
            every { coBrowsingSessionRepository.save(any()) } answers { Mono.just(firstArg()) }
            every {
                coBrowsingSessionReadRepository.updateStatus(
                    id = any(),
                    status = any(),
                    startedAt = any(),
                    endedAt = any(),
                    updatedAt = any(),
                )
            } returns Mono.empty()

            StepVerifier
                .create(coBrowsingService.startSession(command))
                .assertNext { saved ->
                    saved.status shouldBe CoBrowsingStatus.ACTIVE
                    saved.startedAt shouldNotBe null
                }.verifyComplete()

            verify { coBrowsingSessionRepository.save(match { it.status == CoBrowsingStatus.ACTIVE }) }
            verify { coBrowsingNotificationPort.emitSessionUpdate(channelId, any()) }
        }

        "startSession should throw ConflictException when session is not REQUESTED" {
            val channelId = UUID.randomUUID()
            val session = makeSession(channelId, CoBrowsingStatus.ACTIVE)
            val command = StartCoBrowsingCommand(channelId = channelId, sessionId = session.id)

            every { coBrowsingSessionRepository.findByIdAndNotDeleted(session.id) } returns Mono.just(session)

            StepVerifier
                .create(coBrowsingService.startSession(command))
                .expectError(ConflictException::class.java)
                .verify()
        }

        "endSession should transition session to ENDED" {
            val channelId = UUID.randomUUID()
            val session = makeSession(channelId, CoBrowsingStatus.ACTIVE)
            val command = EndCoBrowsingCommand(channelId = channelId, sessionId = session.id)

            every { coBrowsingSessionRepository.findByIdAndNotDeleted(session.id) } returns Mono.just(session)
            every { coBrowsingSessionRepository.save(any()) } answers { Mono.just(firstArg()) }
            every {
                coBrowsingSessionReadRepository.updateStatus(
                    id = any(),
                    status = any(),
                    startedAt = any(),
                    endedAt = any(),
                    updatedAt = any(),
                )
            } returns Mono.empty()

            StepVerifier
                .create(coBrowsingService.endSession(command))
                .assertNext { saved ->
                    saved.status shouldBe CoBrowsingStatus.ENDED
                }.verifyComplete()

            verify { coBrowsingSessionRepository.save(match { it.status == CoBrowsingStatus.ENDED }) }
            verify { coBrowsingNotificationPort.emitSessionUpdate(channelId, any()) }
        }

        "endSession should also work when session is still REQUESTED" {
            val channelId = UUID.randomUUID()
            val session = makeSession(channelId, CoBrowsingStatus.REQUESTED)
            val command = EndCoBrowsingCommand(channelId = channelId, sessionId = session.id)

            every { coBrowsingSessionRepository.findByIdAndNotDeleted(session.id) } returns Mono.just(session)
            every { coBrowsingSessionRepository.save(any()) } answers { Mono.just(firstArg()) }
            every {
                coBrowsingSessionReadRepository.updateStatus(
                    id = any(),
                    status = any(),
                    startedAt = any(),
                    endedAt = any(),
                    updatedAt = any(),
                )
            } returns Mono.empty()

            StepVerifier
                .create(coBrowsingService.endSession(command))
                .assertNext { saved ->
                    saved.status shouldBe CoBrowsingStatus.ENDED
                }.verifyComplete()
        }

        "endSession should throw ConflictException when session is already ENDED" {
            val channelId = UUID.randomUUID()
            val session = makeSession(channelId, CoBrowsingStatus.ENDED)
            val command = EndCoBrowsingCommand(channelId = channelId, sessionId = session.id)

            every { coBrowsingSessionRepository.findByIdAndNotDeleted(session.id) } returns Mono.just(session)

            StepVerifier
                .create(coBrowsingService.endSession(command))
                .expectError(ConflictException::class.java)
                .verify()
        }
    })
