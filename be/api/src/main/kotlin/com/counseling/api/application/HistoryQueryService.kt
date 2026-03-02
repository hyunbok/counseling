package com.counseling.api.application

import com.counseling.api.domain.exception.NotFoundException
import com.counseling.api.port.inbound.HistoryDetail
import com.counseling.api.port.inbound.HistoryDetailCounselNote
import com.counseling.api.port.inbound.HistoryDetailFeedback
import com.counseling.api.port.inbound.HistoryDetailRecording
import com.counseling.api.port.inbound.HistoryFilter
import com.counseling.api.port.inbound.HistoryListItem
import com.counseling.api.port.inbound.HistoryListResult
import com.counseling.api.port.inbound.HistoryQuery
import com.counseling.api.port.outbound.HistoryProjection
import com.counseling.api.port.outbound.HistoryReadRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.UUID

@Service
@Profile("!test")
class HistoryQueryService(
    private val historyReadRepository: HistoryReadRepository,
) : HistoryQuery {
    override fun list(
        tenantId: String,
        filter: HistoryFilter,
    ): Mono<HistoryListResult> =
        historyReadRepository
            .findByTenantId(
                tenantId = tenantId,
                agentId = filter.agentId,
                groupId = filter.groupId,
                dateFrom = filter.dateFrom,
                dateTo = filter.dateTo,
                before = filter.before,
                limit = filter.limit + 1,
            ).map { projections ->
                val hasMore = projections.size > filter.limit
                val trimmed = if (hasMore) projections.dropLast(1) else projections
                HistoryListResult(
                    items = trimmed.map { it.toListItem() },
                    hasMore = hasMore,
                )
            }

    override fun getDetail(
        tenantId: String,
        channelId: UUID,
    ): Mono<HistoryDetail> =
        historyReadRepository
            .findByChannelId(channelId, tenantId)
            .switchIfEmpty(Mono.error(NotFoundException("History not found for channel: $channelId")))
            .map { it.toDetail() }

    private fun HistoryProjection.toListItem(): HistoryListItem =
        HistoryListItem(
            channelId = channelId,
            agentId = agentId,
            agentName = agentName,
            groupId = groupId,
            groupName = groupName,
            customerName = customerName,
            status = status,
            startedAt = startedAt,
            endedAt = endedAt,
            durationSeconds = durationSeconds,
            hasRecording = recording != null,
            hasFeedback = feedback != null,
            feedbackRating = feedback?.rating,
        )

    private fun HistoryProjection.toDetail(): HistoryDetail =
        HistoryDetail(
            channelId = channelId,
            agentId = agentId,
            agentName = agentName,
            groupId = groupId,
            groupName = groupName,
            customerName = customerName,
            customerContact = customerContact,
            status = status,
            startedAt = startedAt,
            endedAt = endedAt,
            durationSeconds = durationSeconds,
            recording =
                recording?.let {
                    HistoryDetailRecording(
                        recordingId = it.recordingId,
                        status = it.status,
                        filePath = it.filePath,
                        startedAt = it.startedAt,
                        stoppedAt = it.stoppedAt,
                    )
                },
            feedback =
                feedback?.let {
                    HistoryDetailFeedback(
                        rating = it.rating,
                        comment = it.comment,
                        createdAt = it.createdAt,
                    )
                },
            counselNote =
                counselNote?.let {
                    HistoryDetailCounselNote(
                        noteId = it.noteId,
                        content = it.content,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt,
                    )
                },
        )
}
