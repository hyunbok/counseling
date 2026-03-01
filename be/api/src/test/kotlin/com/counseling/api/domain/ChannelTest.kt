package com.counseling.api.domain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Instant
import java.util.UUID

class ChannelTest :
    StringSpec({
        val baseInstant = Instant.parse("2026-01-01T00:00:00Z")

        fun createChannel(
            status: ChannelStatus = ChannelStatus.WAITING,
            agentId: UUID? = null,
            deleted: Boolean = false,
        ): Channel =
            Channel(
                id = UUID.randomUUID(),
                agentId = agentId,
                status = status,
                startedAt = null,
                endedAt = null,
                recordingPath = null,
                createdAt = baseInstant,
                updatedAt = baseInstant,
                deleted = deleted,
            )

        "assignAgent() sets agentId" {
            val agentId = UUID.randomUUID()
            val channel = createChannel()
            val updated = channel.assignAgent(agentId)
            updated.agentId shouldBe agentId
        }

        "start() changes status to IN_PROGRESS and sets startedAt" {
            val channel = createChannel(ChannelStatus.WAITING)
            val started = channel.start()
            started.status shouldBe ChannelStatus.IN_PROGRESS
            started.startedAt shouldNotBe null
        }

        "start() returns a new instance (immutability)" {
            val channel = createChannel(ChannelStatus.WAITING)
            val started = channel.start()
            channel.status shouldBe ChannelStatus.WAITING
            started.status shouldBe ChannelStatus.IN_PROGRESS
        }

        "close() changes status to CLOSED and sets endedAt" {
            val channel = createChannel(ChannelStatus.IN_PROGRESS)
            val closed = channel.close()
            closed.status shouldBe ChannelStatus.CLOSED
            closed.endedAt shouldNotBe null
        }

        "isOpen() returns true when WAITING" {
            val channel = createChannel(ChannelStatus.WAITING)
            channel.isOpen() shouldBe true
        }

        "isOpen() returns true when IN_PROGRESS" {
            val channel = createChannel(ChannelStatus.IN_PROGRESS)
            channel.isOpen() shouldBe true
        }

        "isOpen() returns false when CLOSED" {
            val channel = createChannel(ChannelStatus.CLOSED)
            channel.isOpen() shouldBe false
        }

        "isOpen() returns false when deleted" {
            val channel = createChannel(ChannelStatus.WAITING, deleted = true)
            channel.isOpen() shouldBe false
        }
    })
