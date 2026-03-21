package com.counseling.api.adapter.inbound.web.dto

import java.time.Instant

data class HistoryItemResponse(
    val channelId: String,
    val agentId: String?,
    val agentName: String?,
    val groupId: String?,
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
    val recordingId: String,
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
    val noteId: String,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class HistoryDetailResponse(
    val channelId: String,
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
    val recording: HistoryRecordingResponse?,
    val feedback: HistoryFeedbackResponse?,
    val counselNote: HistoryCounselNoteResponse?,
)
