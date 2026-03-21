package com.counseling.api.domain

import java.time.Instant

data class CounselNote(
    val id: String? = null,
    val channelId: String,
    val agentId: String,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deleted: Boolean = false,
) {
    fun updateContent(content: String): CounselNote = copy(content = content, updatedAt = Instant.now())

    fun softDelete(): CounselNote = copy(deleted = true, updatedAt = Instant.now())
}
