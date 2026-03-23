package com.counseling.admin.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("channels")
data class Channel(
    @Id val id: String? = null,
    @Column("agent_id") val agentId: String?,
    val status: ChannelStatus,
    @Column("started_at") val startedAt: Instant?,
    @Column("ended_at") val endedAt: Instant?,
    @Column("created_at") val createdAt: Instant,
    @Column("updated_at") val updatedAt: Instant,
)
