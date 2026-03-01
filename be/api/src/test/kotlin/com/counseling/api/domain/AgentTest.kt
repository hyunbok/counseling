package com.counseling.api.domain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.util.UUID

class AgentTest :
    StringSpec({
        val baseInstant = Instant.parse("2026-01-01T00:00:00Z")

        fun createAgent(
            agentStatus: AgentStatus = AgentStatus.OFFLINE,
            deleted: Boolean = false,
            groupId: UUID? = null,
        ): Agent =
            Agent(
                id = UUID.randomUUID(),
                username = "agent1",
                passwordHash = "hash",
                name = "Test Agent",
                role = AgentRole.COUNSELOR,
                createdAt = baseInstant,
                updatedAt = baseInstant,
                deleted = deleted,
                groupId = groupId,
                agentStatus = agentStatus,
            )

        "updateStatus() changes agentStatus" {
            val agent = createAgent(AgentStatus.OFFLINE)
            val updated = agent.updateStatus(AgentStatus.ONLINE)
            updated.agentStatus shouldBe AgentStatus.ONLINE
        }

        "updateStatus() returns a new instance (immutability)" {
            val agent = createAgent(AgentStatus.OFFLINE)
            val updated = agent.updateStatus(AgentStatus.BUSY)
            agent.agentStatus shouldBe AgentStatus.OFFLINE
            updated.agentStatus shouldBe AgentStatus.BUSY
        }

        "assignToGroup() sets groupId" {
            val groupId = UUID.randomUUID()
            val agent = createAgent()
            val updated = agent.assignToGroup(groupId)
            updated.groupId shouldBe groupId
        }

        "assignToGroup(null) clears groupId" {
            val agent = createAgent(groupId = UUID.randomUUID())
            val updated = agent.assignToGroup(null)
            updated.groupId shouldBe null
        }

        "isAvailable() returns true when ONLINE and not deleted" {
            val agent = createAgent(AgentStatus.ONLINE, deleted = false)
            agent.isAvailable() shouldBe true
        }

        "isAvailable() returns false when OFFLINE" {
            val agent = createAgent(AgentStatus.OFFLINE)
            agent.isAvailable() shouldBe false
        }

        "isAvailable() returns false when BUSY" {
            val agent = createAgent(AgentStatus.BUSY)
            agent.isAvailable() shouldBe false
        }

        "isAvailable() returns false when ONLINE but deleted" {
            val agent = createAgent(AgentStatus.ONLINE, deleted = true)
            agent.isAvailable() shouldBe false
        }
    })
