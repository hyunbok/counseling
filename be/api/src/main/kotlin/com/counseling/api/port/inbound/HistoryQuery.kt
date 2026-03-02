package com.counseling.api.port.inbound

import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

data class HistoryFilter(
    val agentId: UUID? = null,
    val groupId: UUID? = null,
    val dateFrom: Instant? = null,
    val dateTo: Instant? = null,
    val before: Instant? = null,
    val limit: Int = 20,
)

data class HistoryListItem(
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

data class HistoryListResult(
    val items: List<HistoryListItem>,
    val hasMore: Boolean,
)

data class HistoryDetailRecording(
    val recordingId: UUID,
    val status: String,
    val filePath: String?,
    val startedAt: Instant,
    val stoppedAt: Instant?,
)

data class HistoryDetailFeedback(
    val rating: Int,
    val comment: String?,
    val createdAt: Instant,
)

data class HistoryDetailCounselNote(
    val noteId: UUID,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class HistoryDetail(
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
    val recording: HistoryDetailRecording?,
    val feedback: HistoryDetailFeedback?,
    val counselNote: HistoryDetailCounselNote?,
)

interface HistoryQuery {
    fun list(
        tenantId: String,
        filter: HistoryFilter,
    ): Mono<HistoryListResult>

    fun getDetail(
        tenantId: String,
        channelId: UUID,
    ): Mono<HistoryDetail>
}
