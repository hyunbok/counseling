package com.counseling.api.domain.event

import com.counseling.api.domain.Agent
import com.counseling.api.domain.AgentRole
import com.counseling.api.domain.AgentStatus
import com.counseling.api.domain.Channel
import com.counseling.api.domain.ChannelStatus
import com.counseling.api.domain.Feedback
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Instant
import java.util.UUID

class DomainEventTest :
    StringSpec({
        val baseInstant = Instant.parse("2026-01-01T00:00:00Z")

        fun createAgent(): Agent =
            Agent(
                id = UUID.randomUUID(),
                username = "agent1",
                passwordHash = "hash",
                name = "Test Agent",
                role = AgentRole.COUNSELOR,
                createdAt = baseInstant,
                updatedAt = baseInstant,
                agentStatus = AgentStatus.ONLINE,
            )

        fun createChannel(): Channel =
            Channel(
                id = UUID.randomUUID(),
                agentId = null,
                status = ChannelStatus.WAITING,
                startedAt = null,
                endedAt = null,
                recordingPath = null,
                createdAt = baseInstant,
                updatedAt = baseInstant,
            )

        fun createFeedback(): Feedback =
            Feedback(
                id = UUID.randomUUID(),
                channelId = UUID.randomUUID(),
                rating = 4,
                comment = "Great service",
                createdAt = baseInstant,
            )

        "AgentEvent.StatusChanged has agent and occurredAt" {
            val agent = createAgent()
            val event = AgentEvent.StatusChanged(agent = agent)
            event.agent shouldBe agent
            event.occurredAt shouldNotBe null
        }

        "AgentEvent.AssignedToGroup has agent and occurredAt" {
            val agent = createAgent()
            val event = AgentEvent.AssignedToGroup(agent = agent)
            event.agent shouldBe agent
            event.occurredAt shouldNotBe null
        }

        "ChannelEvent.Created has channel and occurredAt" {
            val channel = createChannel()
            val event = ChannelEvent.Created(channel = channel)
            event.channel shouldBe channel
            event.occurredAt shouldNotBe null
        }

        "ChannelEvent.Started has channel and occurredAt" {
            val channel = createChannel()
            val event = ChannelEvent.Started(channel = channel)
            event.channel shouldBe channel
            event.occurredAt shouldNotBe null
        }

        "ChannelEvent.Closed has channel and occurredAt" {
            val channel = createChannel()
            val event = ChannelEvent.Closed(channel = channel)
            event.channel shouldBe channel
            event.occurredAt shouldNotBe null
        }

        "FeedbackEvent Submitted has feedback and occurredAt" {
            val feedback = createFeedback()
            val event = FeedbackEvent.Submitted(feedback = feedback)
            event.feedback shouldBe feedback
            event.occurredAt shouldNotBe null
        }

        "AgentEvent.StatusChanged is a DomainEvent" {
            val event: DomainEvent = AgentEvent.StatusChanged(agent = createAgent())
            event.occurredAt shouldNotBe null
        }

        "ChannelEvent.Created is a DomainEvent" {
            val event: DomainEvent = ChannelEvent.Created(channel = createChannel())
            event.occurredAt shouldNotBe null
        }
    })
