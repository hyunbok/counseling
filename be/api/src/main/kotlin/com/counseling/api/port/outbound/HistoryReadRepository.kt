package com.counseling.api.port.outbound

import reactor.core.publisher.Mono
import java.time.Instant

data class HistoryProjection(
    val channelId: String,
    val tenantId: String,
    val agentId: String?,
    val agentName: String?,
    val groupId: String?,
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
    val recordingId: String,
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
    val noteId: String,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

interface HistoryReadRepository {
    fun upsert(projection: HistoryProjection): Mono<Void>

    fun updateRecording(
        channelId: String,
        tenantId: String,
        recording: RecordingProjection,
    ): Mono<Void>

    fun updateFeedback(
        channelId: String,
        tenantId: String,
        feedback: FeedbackProjection,
    ): Mono<Void>

    fun updateCounselNote(
        channelId: String,
        tenantId: String,
        counselNote: CounselNoteProjection,
    ): Mono<Void>

    fun updateStatus(
        channelId: String,
        tenantId: String,
        status: String,
        endedAt: Instant?,
        durationSeconds: Long?,
    ): Mono<Void>

    fun findByTenantId(
        tenantId: String,
        agentId: String?,
        groupId: String?,
        dateFrom: Instant?,
        dateTo: Instant?,
        before: Instant?,
        limit: Int,
    ): Mono<List<HistoryProjection>>

    fun findByChannelId(
        channelId: String,
        tenantId: String,
    ): Mono<HistoryProjection>
}
