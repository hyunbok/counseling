package com.counseling.api.domain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Instant
import java.util.UUID

class RecordingTest :
    StringSpec({
        val now = Instant.parse("2026-01-01T00:00:00Z")

        fun makeRecording(status: RecordingStatus = RecordingStatus.RECORDING): Recording =
            Recording(
                id = UUID.randomUUID(),
                channelId = UUID.randomUUID(),
                egressId = "egress-123",
                status = status,
                filePath = null,
                startedAt = now,
                stoppedAt = null,
                createdAt = now,
                updatedAt = now,
            )

        "stop() sets status to STOPPED and stoppedAt" {
            val recording = makeRecording()
            val stopped = recording.stop(null)

            stopped.status shouldBe RecordingStatus.STOPPED
            stopped.stoppedAt shouldNotBe null
        }

        "stop() sets filePath" {
            val recording = makeRecording()
            val filePath = "/recordings/tenant/recording.mp4"
            val stopped = recording.stop(filePath)

            stopped.filePath shouldBe filePath
        }

        "markFailed() sets status to FAILED" {
            val recording = makeRecording()
            val failed = recording.markFailed()

            failed.status shouldBe RecordingStatus.FAILED
        }

        "isActive() returns true when RECORDING" {
            val recording = makeRecording(RecordingStatus.RECORDING)

            recording.isActive() shouldBe true
        }

        "isActive() returns false when STOPPED" {
            val recording = makeRecording(RecordingStatus.STOPPED)

            recording.isActive() shouldBe false
        }

        "isActive() returns false when FAILED" {
            val recording = makeRecording(RecordingStatus.FAILED)

            recording.isActive() shouldBe false
        }
    })
