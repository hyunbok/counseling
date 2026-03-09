package com.counseling.api.adapter.inbound.web.controller

import com.counseling.api.adapter.inbound.web.dto.CustomerDeviceResponse
import com.counseling.api.adapter.inbound.web.dto.DashboardRecentItemResponse
import com.counseling.api.adapter.inbound.web.dto.DashboardSummaryResponse
import com.counseling.api.adapter.inbound.web.dto.HistoryCounselNoteResponse
import com.counseling.api.adapter.inbound.web.dto.HistoryDetailResponse
import com.counseling.api.adapter.inbound.web.dto.HistoryFeedbackResponse
import com.counseling.api.adapter.inbound.web.dto.HistoryItemResponse
import com.counseling.api.adapter.inbound.web.dto.HistoryListResponse
import com.counseling.api.adapter.inbound.web.dto.HistoryRecordingResponse
import com.counseling.api.domain.AgentRole
import com.counseling.api.domain.auth.AuthenticatedAgent
import com.counseling.api.domain.exception.UnauthorizedException
import com.counseling.api.port.inbound.HistoryDetail
import com.counseling.api.port.inbound.HistoryFilter
import com.counseling.api.port.inbound.HistoryListItem
import com.counseling.api.port.inbound.HistoryQuery
import com.counseling.api.port.inbound.RecordingStreamUseCase
import org.springframework.context.annotation.Profile
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@RestController
@RequestMapping("/api/history")
@Profile("!test")
class HistoryController(
    private val historyQuery: HistoryQuery,
    private val recordingStreamUseCase: RecordingStreamUseCase,
) {
    @GetMapping("/dashboard-summary")
    fun getDashboardSummary(): Mono<DashboardSummaryResponse> =
        authenticatedAgent().flatMap { agent ->
            val zone = ZoneId.systemDefault()
            val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant()
            historyQuery
                .getDashboardSummary(agent.tenantId, agent.agentId, todayStart)
                .map { summary ->
                    DashboardSummaryResponse(
                        todayCount = summary.todayCount,
                        totalDurationSeconds = summary.totalDurationSeconds,
                        avgDurationSeconds = summary.avgDurationSeconds,
                        recentItems =
                            summary.recentItems.map {
                                DashboardRecentItemResponse(
                                    channelId = it.channelId,
                                    customerName = it.customerName,
                                    status = it.status,
                                    startedAt = it.startedAt,
                                    durationSeconds = it.durationSeconds,
                                    feedbackRating = it.feedbackRating,
                                )
                            },
                    )
                }
        }

    @GetMapping
    fun listHistory(
        @RequestParam(required = false) agentId: UUID?,
        @RequestParam(required = false) groupId: UUID?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) customerName: String?,
        @RequestParam(required = false) dateFrom: Instant?,
        @RequestParam(required = false) dateTo: Instant?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): Mono<HistoryListResponse> =
        authenticatedAgent().flatMap { agent ->
            val effectiveAgentId =
                if (agent.role == AgentRole.COUNSELOR) {
                    agent.agentId
                } else {
                    agentId
                }
            val filter =
                HistoryFilter(
                    agentId = effectiveAgentId,
                    groupId = groupId,
                    status = status,
                    customerName = customerName,
                    dateFrom = dateFrom,
                    dateTo = dateTo,
                    page = page.coerceAtLeast(0),
                    size = size.coerceIn(1, 100),
                )
            historyQuery
                .list(agent.tenantId, filter)
                .map { result ->
                    HistoryListResponse(
                        items = result.items.map { it.toResponse() },
                        totalCount = result.totalCount,
                        page = result.page,
                        size = result.size,
                        totalPages = result.totalPages,
                    )
                }
        }

    @GetMapping("/{channelId}")
    fun getDetail(
        @PathVariable channelId: UUID,
    ): Mono<HistoryDetailResponse> =
        authenticatedAgent().flatMap { agent ->
            historyQuery
                .getDetail(agent.tenantId, channelId)
                .flatMap { detail ->
                    if (agent.role == AgentRole.COUNSELOR && detail.agentId != agent.agentId) {
                        Mono.error(UnauthorizedException("Not authorized to view this history"))
                    } else {
                        Mono.just(detail.toResponse())
                    }
                }
        }

    @GetMapping("/{channelId}/recording/{recordingId}")
    fun streamRecording(
        @PathVariable channelId: UUID,
        @PathVariable recordingId: UUID,
        @RequestHeader(value = HttpHeaders.RANGE, required = false) rangeHeader: String?,
    ): Mono<ResponseEntity<Resource>> =
        authenticatedAgent().flatMap { agent ->
            recordingStreamUseCase
                .getRecordingResource(channelId, recordingId, agent.agentId)
                .map { recordingResource ->
                    val headers = HttpHeaders()
                    headers.contentType = MediaType.parseMediaType("video/mp4")
                    headers.setContentDispositionFormData("inline", recordingResource.filename)

                    if (rangeHeader != null) {
                        val range = parseRange(rangeHeader, recordingResource.contentLength)
                        val start = range.first
                        val end = range.second
                        val contentLength = end - start + 1
                        headers[HttpHeaders.CONTENT_RANGE] =
                            "bytes $start-$end/${recordingResource.contentLength}"
                        headers.contentLength = contentLength
                        ResponseEntity
                            .status(HttpStatus.PARTIAL_CONTENT)
                            .headers(headers)
                            .body(recordingResource.resource)
                    } else {
                        headers.contentLength = recordingResource.contentLength
                        ResponseEntity
                            .ok()
                            .headers(headers)
                            .body(recordingResource.resource)
                    }
                }
        }

    private fun parseRange(
        rangeHeader: String,
        contentLength: Long,
    ): Pair<Long, Long> {
        val rangeValue = rangeHeader.removePrefix("bytes=")
        val parts = rangeValue.split("-")
        val start = if (parts[0].isNotBlank()) parts[0].toLong() else 0L
        val end =
            if (parts.size > 1 && parts[1].isNotBlank()) {
                parts[1].toLong()
            } else {
                contentLength - 1
            }
        return Pair(start, end)
    }

    private fun authenticatedAgent(): Mono<AuthenticatedAgent> =
        ReactiveSecurityContextHolder
            .getContext()
            .flatMap { ctx ->
                val principal = ctx.authentication?.principal
                if (principal is AuthenticatedAgent) {
                    Mono.just(principal)
                } else {
                    Mono.error(UnauthorizedException("Not authenticated"))
                }
            }

    private fun HistoryListItem.toResponse(): HistoryItemResponse =
        HistoryItemResponse(
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
            hasRecording = hasRecording,
            hasFeedback = hasFeedback,
            feedbackRating = feedbackRating,
        )

    private fun HistoryDetail.toResponse(): HistoryDetailResponse =
        HistoryDetailResponse(
            channelId = channelId,
            agentId = agentId,
            agentName = agentName,
            groupId = groupId,
            groupName = groupName,
            customerName = customerName,
            customerContact = customerContact,
            customerDevice =
                customerDevice?.let {
                    CustomerDeviceResponse(
                        deviceType = it.deviceType,
                        deviceBrand = it.deviceBrand,
                        osName = it.osName,
                        osVersion = it.osVersion,
                        browserName = it.browserName,
                        browserVersion = it.browserVersion,
                    )
                },
            status = status,
            startedAt = startedAt,
            endedAt = endedAt,
            durationSeconds = durationSeconds,
            recording =
                recording?.let {
                    HistoryRecordingResponse(
                        recordingId = it.recordingId,
                        status = it.status,
                        startedAt = it.startedAt,
                        stoppedAt = it.stoppedAt,
                    )
                },
            feedback =
                feedback?.let {
                    HistoryFeedbackResponse(
                        rating = it.rating,
                        comment = it.comment,
                        createdAt = it.createdAt,
                    )
                },
            counselNote =
                counselNote?.let {
                    HistoryCounselNoteResponse(
                        noteId = it.noteId,
                        content = it.content,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt,
                    )
                },
        )
}
