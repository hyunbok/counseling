package com.counseling.api.application

import com.counseling.api.domain.exception.NotFoundException
import com.counseling.api.port.inbound.DashboardSummary
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
import java.time.Instant
import java.util.UUID

@Service
@Profile("!test")
class HistoryQueryService(
    private val historyReadRepository: HistoryReadRepository,
) : HistoryQuery {
    override fun list(
        tenantId: String,
        filter: HistoryFilter,
    ): Mono<HistoryListResult> {
        val skip = filter.page * filter.size
        val findMono =
            historyReadRepository.findByTenantId(
                tenantId = tenantId,
                agentId = filter.agentId,
                groupId = filter.groupId,
                status = filter.status,
                customerName = filter.customerName,
                dateFrom = filter.dateFrom,
                dateTo = filter.dateTo,
                skip = skip,
                limit = filter.size,
            )
        val countMono =
            historyReadRepository.countByTenantId(
                tenantId = tenantId,
                agentId = filter.agentId,
                groupId = filter.groupId,
                status = filter.status,
                customerName = filter.customerName,
                dateFrom = filter.dateFrom,
                dateTo = filter.dateTo,
            )
        return Mono
            .zip(findMono, countMono)
            .map { tuple ->
                val projections = tuple.t1
                val totalCount = tuple.t2
                val totalPages = ((totalCount + filter.size - 1) / filter.size).toInt()
                HistoryListResult(
                    items = projections.map { it.toListItem() },
                    totalCount = totalCount,
                    page = filter.page,
                    size = filter.size,
                    totalPages = totalPages,
                )
            }
    }

    override fun getDetail(
        tenantId: String,
        channelId: UUID,
    ): Mono<HistoryDetail> =
        historyReadRepository
            .findByChannelId(channelId, tenantId)
            .switchIfEmpty(Mono.error(NotFoundException("History not found for channel: $channelId")))
            .map { it.toDetail() }

    override fun getDashboardSummary(
        tenantId: String,
        agentId: UUID,
        todayStart: Instant,
    ): Mono<DashboardSummary> =
        historyReadRepository
            .findByTenantId(
                tenantId = tenantId,
                agentId = agentId,
                groupId = null,
                status = null,
                customerName = null,
                dateFrom = todayStart,
                dateTo = null,
                skip = 0,
                limit = 100,
            ).map { projections ->
                val items = projections.map { it.toListItem() }
                val withDuration = items.filter { it.durationSeconds != null }
                val avgDuration =
                    if (withDuration.isNotEmpty()) {
                        withDuration.sumOf { it.durationSeconds!! } / withDuration.size
                    } else {
                        null
                    }
                DashboardSummary(
                    todayCount = items.size,
                    avgDurationSeconds = avgDuration,
                    recentItems = items.take(5),
                )
            }

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
            customerDevice = customerDevice,
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
