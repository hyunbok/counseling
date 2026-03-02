package com.counseling.api.port.outbound

import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

data class HistoryProjection(
    val channelId: UUID,
    val tenantId: String,
    val agentId: UUID?,
    val agentName: String?,
    val groupId: UUID?,
    val groupName: String?,
    val customerName: String?,
    val customerContact: String?,
    val status: String,
    val startedAt: Instant?,
    val endedAt: Instant?,
    val durationSeconds: Long?,
    val recording: RecordingProjection?,
    val feedback: FeedbackProjection?,
    val counselNote: CounselNoteProjection?,
)

data class RecordingProjection(
    val recordingId: UUID,
    val status: String,
    val filePath: String?,
    val startedAt: Instant,
    val stoppedAt: Instant?,
)

data class FeedbackProjection(
    val rating: Int,
    val comment: String?,
    val createdAt: Instant,
)

data class CounselNoteProjection(
    val noteId: UUID,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

interface HistoryReadRepository {
    fun upsert(projection: HistoryProjection): Mono<Void>

    fun updateRecording(
        channelId: UUID,
        tenantId: String,
        recording: RecordingProjection,
    ): Mono<Void>

    fun updateFeedback(
        channelId: UUID,
        tenantId: String,
        feedback: FeedbackProjection,
    ): Mono<Void>

    fun updateCounselNote(
        channelId: UUID,
        tenantId: String,
        counselNote: CounselNoteProjection,
    ): Mono<Void>

    fun updateStatus(
        channelId: UUID,
        tenantId: String,
        status: String,
        endedAt: Instant?,
        durationSeconds: Long?,
    ): Mono<Void>

    fun findByTenantId(
        tenantId: String,
        agentId: UUID?,
        groupId: UUID?,
        dateFrom: Instant?,
        dateTo: Instant?,
        before: Instant?,
        limit: Int,
    ): Mono<List<HistoryProjection>>

    fun findByChannelId(
        channelId: UUID,
        tenantId: String,
    ): Mono<HistoryProjection>
}
