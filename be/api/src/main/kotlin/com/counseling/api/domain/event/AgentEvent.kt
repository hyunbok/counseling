package com.counseling.api.domain.event

import com.counseling.api.domain.Agent
import java.time.Instant

sealed class AgentEvent : DomainEvent {
    data class StatusChanged(
        val agent: Agent,
        override val occurredAt: Instant = Instant.now(),
    ) : AgentEvent()

    data class AssignedToGroup(
        val agent: Agent,
        override val occurredAt: Instant = Instant.now(),
    ) : AgentEvent()
}
