package com.counseling.api.adapter.inbound.web.dto

import java.time.Instant
import java.util.UUID

data class HistoryItemResponse(
    val channelId: UUID,
    val agentId: UUID?,
    val agentName: String?,
    val groupId: UUID?,
    val groupName: String?,
    val customerName: String?,
    val status: String,
    val startedAt: Instant?,
    val endedAt: Instant?,
    val durationSeconds: Long?,
    val hasRecording: Boolean,
    val hasFeedback: Boolean,
    val feedbackRating: Int?,
)

data class HistoryListResponse(
    val items: List<HistoryItemResponse>,
    val hasMore: Boolean,
)

data class HistoryRecordingResponse(
    val recordingId: UUID,
    val status: String,
    val startedAt: Instant,
    val stoppedAt: Instant?,
)

data class HistoryFeedbackResponse(
    val rating: Int,
    val comment: String?,
    val createdAt: Instant,
)

data class HistoryCounselNoteResponse(
    val noteId: UUID,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class HistoryDetailResponse(
    val channelId: UUID,
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
    val recording: HistoryRecordingResponse?,
    val feedback: HistoryFeedbackResponse?,
    val counselNote: HistoryCounselNoteResponse?,
)
