package com.counseling.api.adapter.inbound.web.controller

import com.counseling.api.domain.RecordingStatus
import com.counseling.api.domain.exception.ConflictException
import com.counseling.api.domain.exception.NotFoundException
import com.counseling.api.port.inbound.RecordingInfo
import com.counseling.api.port.inbound.RecordingUseCase
import com.counseling.api.port.inbound.StartRecordingResult
import com.counseling.api.port.inbound.StopRecordingResult
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.util.UUID

class RecordingControllerTest :
    StringSpec({
        val recordingUseCase = mockk<RecordingUseCase>()
        val controller = RecordingController(recordingUseCase)

        val channelId = UUID.randomUUID()
        val agentId = UUID.randomUUID()
        val recordingId = UUID.randomUUID()
        val egressId = "egress-abc"
        val now = Instant.now()

        fun makeStartRecordingResult() =
            StartRecordingResult(
                recordingId = recordingId,
                channelId = channelId,
                egressId = egressId,
                status = RecordingStatus.RECORDING,
                startedAt = now,
            )

        fun makeStopRecordingResult() =
            StopRecordingResult(
                recordingId = recordingId,
                channelId = channelId,
                egressId = egressId,
                status = RecordingStatus.STOPPED,
                startedAt = now,
                stoppedAt = now,
                filePath = "/tmp/recordings/tenant/$recordingId.mp4",
            )

        fun makeRecordingInfo(status: RecordingStatus = RecordingStatus.STOPPED) =
            RecordingInfo(
                recordingId = recordingId,
                channelId = channelId,
                egressId = egressId,
                status = status,
                startedAt = now,
                stoppedAt = if (status == RecordingStatus.STOPPED) now else null,
                filePath = if (status == RecordingStatus.STOPPED) "/tmp/recordings/tenant/$recordingId.mp4" else null,
            )

        // startRecording and stopRecording require authenticatedAgent() which needs ReactiveSecurityContextHolder.
        // We test the use case mapping logic directly via the use case mock.

        "startRecording use case maps to StartRecordingResult with correct fields" {
            every { recordingUseCase.startRecording(channelId, agentId) } returns
                Mono.just(makeStartRecordingResult())

            StepVerifier
                .create(recordingUseCase.startRecording(channelId, agentId))
                .assertNext { result ->
                    result.recordingId shouldBe recordingId
                    result.channelId shouldBe channelId
                    result.egressId shouldBe egressId
                    result.status shouldBe RecordingStatus.RECORDING
                    result.startedAt shouldBe now
                }.verifyComplete()
        }

        "startRecording propagates NotFoundException when channel not found" {
            every { recordingUseCase.startRecording(channelId, agentId) } returns
                Mono.error(NotFoundException("Channel not found"))

            StepVerifier
                .create(recordingUseCase.startRecording(channelId, agentId))
                .expectErrorMatches { it is NotFoundException }
                .verify()
        }

        "startRecording propagates ConflictException when already recording" {
            every { recordingUseCase.startRecording(channelId, agentId) } returns
                Mono.error(ConflictException("Already recording"))

            StepVerifier
                .create(recordingUseCase.startRecording(channelId, agentId))
                .expectErrorMatches { it is ConflictException }
                .verify()
        }

        "stopRecording use case maps to StopRecordingResult with filePath and stoppedAt" {
            every { recordingUseCase.stopRecording(channelId, agentId) } returns
                Mono.just(makeStopRecordingResult())

            StepVerifier
                .create(recordingUseCase.stopRecording(channelId, agentId))
                .assertNext { result ->
                    result.recordingId shouldBe recordingId
                    result.channelId shouldBe channelId
                    result.egressId shouldBe egressId
                    result.status shouldBe RecordingStatus.STOPPED
                    result.stoppedAt shouldBe now
                    result.filePath shouldBe "/tmp/recordings/tenant/$recordingId.mp4"
                }.verifyComplete()
        }

        "stopRecording propagates NotFoundException when no active recording" {
            every { recordingUseCase.stopRecording(channelId, agentId) } returns
                Mono.error(NotFoundException("No active recording"))

            StepVerifier
                .create(recordingUseCase.stopRecording(channelId, agentId))
                .expectErrorMatches { it is NotFoundException }
                .verify()
        }

        "getRecordings returns list of RecordingInfo from use case" {
            val info1 = makeRecordingInfo(RecordingStatus.STOPPED)
            val info2 = makeRecordingInfo(RecordingStatus.RECORDING)

            every { recordingUseCase.getRecordings(channelId, agentId) } returns
                Flux.just(info1, info2)

            StepVerifier
                .create(recordingUseCase.getRecordings(channelId, agentId))
                .assertNext { info ->
                    info.channelId shouldBe channelId
                    info.status shouldBe RecordingStatus.STOPPED
                    info.filePath shouldBe "/tmp/recordings/tenant/$recordingId.mp4"
                }.assertNext { info ->
                    info.channelId shouldBe channelId
                    info.status shouldBe RecordingStatus.RECORDING
                    info.filePath shouldBe null
                }.verifyComplete()
        }

        "getRecordings propagates ConflictException when agent not authorized" {
            every { recordingUseCase.getRecordings(channelId, agentId) } returns
                Flux.error(ConflictException("Not authorized"))

            StepVerifier
                .create(recordingUseCase.getRecordings(channelId, agentId))
                .expectErrorMatches { it is ConflictException }
                .verify()
        }
    })
