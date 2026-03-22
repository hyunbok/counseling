package com.counseling.api.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant

@Table("counsel_notes")
data class CounselNote(
    @Id val id: String? = null,
    @Column("channel_id") val channelId: String,
    @Column("agent_id") val agentId: String,
    val content: String,
    @Column("created_at") val createdAt: Instant,
    @Column("updated_at") val updatedAt: Instant,
    val deleted: Boolean = false,
) {
    fun updateContent(content: String): CounselNote = copy(content = content, updatedAt = Instant.now())

    fun softDelete(): CounselNote = copy(deleted = true, updatedAt = Instant.now())
}
