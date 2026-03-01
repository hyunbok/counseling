package com.counseling.api.domain

import java.time.Instant
import java.util.UUID

data class CounselNote(
    val id: UUID,
    val channelId: UUID,
    val agentId: UUID,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deleted: Boolean = false,
) {
    fun updateContent(content: String): CounselNote = copy(content = content, updatedAt = Instant.now())

    fun softDelete(): CounselNote = copy(deleted = true, updatedAt = Instant.now())
}
