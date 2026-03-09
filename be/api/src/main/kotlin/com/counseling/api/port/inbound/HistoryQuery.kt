package com.counseling.api.port.inbound

import com.counseling.api.domain.CustomerDeviceInfo
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

data class HistoryFilter(
    val agentId: UUID? = null,
    val groupId: UUID? = null,
    val status: String? = null,
    val customerName: String? = null,
    val dateFrom: Instant? = null,
    val dateTo: Instant? = null,
    val page: Int = 0,
    val size: Int = 20,
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
    val totalCount: Long,
    val page: Int,
    val size: Int,
    val totalPages: Int,
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
    val customerDevice: CustomerDeviceInfo?,
    val status: String,
    val startedAt: Instant?,
    val endedAt: Instant?,
    val durationSeconds: Long?,
    val recording: HistoryDetailRecording?,
    val feedback: HistoryDetailFeedback?,
    val counselNote: HistoryDetailCounselNote?,
)

data class DashboardSummary(
    val todayCount: Int,
    val totalDurationSeconds: Long?,
    val avgDurationSeconds: Long?,
    val recentItems: List<HistoryListItem>,
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

    fun getDashboardSummary(
        tenantId: String,
        agentId: UUID,
        todayStart: Instant,
    ): Mono<DashboardSummary>
}
