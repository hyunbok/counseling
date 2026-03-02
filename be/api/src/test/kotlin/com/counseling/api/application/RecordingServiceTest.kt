package com.counseling.api.application

import com.counseling.api.config.RecordingProperties
import com.counseling.api.domain.Channel
import com.counseling.api.domain.ChannelStatus
import com.counseling.api.domain.Recording
import com.counseling.api.domain.RecordingStatus
import com.counseling.api.domain.TenantContext
import com.counseling.api.domain.exception.ConflictException
import com.counseling.api.domain.exception.NotFoundException
import com.counseling.api.port.outbound.ChannelRepository
import com.counseling.api.port.outbound.EgressStartResult
import com.counseling.api.port.outbound.LiveKitEgressPort
import com.counseling.api.port.outbound.RecordingRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import reactor.util.context.Context
import java.time.Instant
import java.util.UUID

class RecordingServiceTest :
    StringSpec({
        val recordingRepository = mockk<RecordingRepository>()
        val channelRepository = mockk<ChannelRepository>()
        val liveKitEgressPort = mockk<LiveKitEgressPort>()
        val recordingProperties =
            RecordingProperties(
                basePath = "/tmp/recordings",
                fileFormat = "mp4",
            )
        val recordingService =
            RecordingService(
                recordingRepository,
                channelRepository,
                liveKitEgressPort,
                recordingProperties,
            )

        val tenantId = "tenant-test"
        val tenantContext: Context = TenantContext.withTenantId(Context.empty(), tenantId)

        afterEach { clearAllMocks() }

        fun makeChannel(
            agentId: UUID,
            roomName: String? = "test-room",
            status: ChannelStatus = ChannelStatus.IN_PROGRESS,
        ): Channel {
            val now = Instant.now()
            return Channel(
                id = UUID.randomUUID(),
                agentId = agentId,
                status = status,
                startedAt = now,
                endedAt = null,
                recordingPath = null,
                livekitRoomName = roomName,
                createdAt = now,
                updatedAt = now,
            )
        }

        fun makeRecording(
            channelId: UUID,
            status: RecordingStatus = RecordingStatus.RECORDING,
        ): Recording {
            val now = Instant.now()
            return Recording(
                id = UUID.randomUUID(),
                channelId = channelId,
                egressId = "egress-456",
                status = status,
                filePath = "/tmp/recordings/$tenantId/${UUID.randomUUID()}.mp4",
                startedAt = now,
                stoppedAt = null,
                createdAt = now,
                updatedAt = now,
            )
        }

        "startRecording creates recording and returns result" {
            val agentId = UUID.randomUUID()
            val channel = makeChannel(agentId = agentId, roomName = "test-room")
            val egressResult = EgressStartResult(egressId = "egress-789")

            every { channelRepository.findByIdAndNotDeleted(channel.id) } returns Mono.just(channel)
            every { recordingRepository.findActiveByChannelId(channel.id) } returns Mono.empty()
            every { liveKitEgressPort.startRoomCompositeEgress("test-room", any()) } returns
                Mono.just(egressResult)
            every { recordingRepository.save(any()) } answers { Mono.just(firstArg()) }

            StepVerifier
                .create(
                    recordingService
                        .startRecording(channel.id, agentId)
                        .contextWrite(tenantContext),
                ).assertNext { result ->
                    result.channelId shouldBe channel.id
                    result.egressId shouldBe "egress-789"
                    result.status shouldBe RecordingStatus.RECORDING
                }.verifyComplete()

            verify { recordingRepository.save(any()) }
        }

        "startRecording throws NotFoundException when channel not found" {
            val channelId = UUID.randomUUID()
            val agentId = UUID.randomUUID()

            every { channelRepository.findByIdAndNotDeleted(channelId) } returns Mono.empty()

            StepVerifier
                .create(
                    recordingService
                        .startRecording(channelId, agentId)
                        .contextWrite(tenantContext),
                ).expectErrorMatches { it is NotFoundException }
                .verify()
        }

        "startRecording throws ConflictException when channel not IN_PROGRESS" {
            val agentId = UUID.randomUUID()
            val channel = makeChannel(agentId = agentId, status = ChannelStatus.CLOSED)

            every { channelRepository.findByIdAndNotDeleted(channel.id) } returns Mono.just(channel)

            StepVerifier
                .create(
                    recordingService
                        .startRecording(channel.id, agentId)
                        .contextWrite(tenantContext),
                ).expectErrorMatches { it is ConflictException }
                .verify()
        }

        "startRecording throws ConflictException when agent not authorized" {
            val agentId = UUID.randomUUID()
            val otherAgentId = UUID.randomUUID()
            val channel = makeChannel(agentId = otherAgentId)

            every { channelRepository.findByIdAndNotDeleted(channel.id) } returns Mono.just(channel)

            StepVerifier
                .create(
                    recordingService
                        .startRecording(channel.id, agentId)
                        .contextWrite(tenantContext),
                ).expectErrorMatches { it is ConflictException }
                .verify()
        }

        "startRecording throws ConflictException when no LiveKit room" {
            val agentId = UUID.randomUUID()
            val channel = makeChannel(agentId = agentId, roomName = null)

            every { channelRepository.findByIdAndNotDeleted(channel.id) } returns Mono.just(channel)

            StepVerifier
                .create(
                    recordingService
                        .startRecording(channel.id, agentId)
                        .contextWrite(tenantContext),
                ).expectErrorMatches { it is ConflictException }
                .verify()
        }

        "startRecording throws ConflictException when already recording" {
            val agentId = UUID.randomUUID()
            val channel = makeChannel(agentId = agentId)
            val activeRecording = makeRecording(channelId = channel.id)

            every { channelRepository.findByIdAndNotDeleted(channel.id) } returns Mono.just(channel)
            every { recordingRepository.findActiveByChannelId(channel.id) } returns Mono.just(activeRecording)

            StepVerifier
                .create(
                    recordingService
                        .startRecording(channel.id, agentId)
                        .contextWrite(tenantContext),
                ).expectErrorMatches { it is ConflictException }
                .verify()
        }

        "stopRecording stops egress and returns result" {
            val agentId = UUID.randomUUID()
            val channel = makeChannel(agentId = agentId)
            val activeRecording = makeRecording(channelId = channel.id)

            every { channelRepository.findByIdAndNotDeleted(channel.id) } returns Mono.just(channel)
            every { recordingRepository.findActiveByChannelId(channel.id) } returns Mono.just(activeRecording)
            every { liveKitEgressPort.stopEgress(activeRecording.egressId) } returns Mono.empty()
            every { recordingRepository.save(any()) } answers { Mono.just(firstArg()) }
            every { channelRepository.save(any()) } answers { Mono.just(firstArg()) }

            StepVerifier
                .create(
                    recordingService
                        .stopRecording(channel.id, agentId)
                        .contextWrite(tenantContext),
                ).assertNext { result ->
                    result.channelId shouldBe channel.id
                    result.egressId shouldBe activeRecording.egressId
                    result.status shouldBe RecordingStatus.STOPPED
                }.verifyComplete()

            verify { liveKitEgressPort.stopEgress(activeRecording.egressId) }
            verify { recordingRepository.save(match { it.status == RecordingStatus.STOPPED }) }
            verify { channelRepository.save(match { it.recordingPath != null }) }
        }

        "stopRecording throws NotFoundException when no active recording" {
            val agentId = UUID.randomUUID()
            val channel = makeChannel(agentId = agentId)

            every { channelRepository.findByIdAndNotDeleted(channel.id) } returns Mono.just(channel)
            every { recordingRepository.findActiveByChannelId(channel.id) } returns Mono.empty()

            StepVerifier
                .create(
                    recordingService
                        .stopRecording(channel.id, agentId)
                        .contextWrite(tenantContext),
                ).expectErrorMatches { it is NotFoundException }
                .verify()
        }

        "getRecordings returns list of recordings for channel" {
            val agentId = UUID.randomUUID()
            val channel = makeChannel(agentId = agentId)
            val recording1 = makeRecording(channelId = channel.id, status = RecordingStatus.STOPPED)
            val recording2 = makeRecording(channelId = channel.id, status = RecordingStatus.RECORDING)

            every { channelRepository.findByIdAndNotDeleted(channel.id) } returns Mono.just(channel)
            every { recordingRepository.findAllByChannelIdAndNotDeleted(channel.id) } returns
                Flux.just(recording1, recording2)

            StepVerifier
                .create(
                    recordingService
                        .getRecordings(channel.id, agentId)
                        .contextWrite(tenantContext),
                ).assertNext { info ->
                    info.channelId shouldBe channel.id
                    info.status shouldBe RecordingStatus.STOPPED
                }.assertNext { info ->
                    info.channelId shouldBe channel.id
                    info.status shouldBe RecordingStatus.RECORDING
                }.verifyComplete()
        }

        "getRecordings throws ConflictException when agent not authorized" {
            val agentId = UUID.randomUUID()
            val otherAgentId = UUID.randomUUID()
            val channel = makeChannel(agentId = otherAgentId)

            every { channelRepository.findByIdAndNotDeleted(channel.id) } returns Mono.just(channel)

            StepVerifier
                .create(
                    recordingService
                        .getRecordings(channel.id, agentId)
                        .contextWrite(tenantContext),
                ).expectErrorMatches { it is ConflictException }
                .verify()
        }
    })
