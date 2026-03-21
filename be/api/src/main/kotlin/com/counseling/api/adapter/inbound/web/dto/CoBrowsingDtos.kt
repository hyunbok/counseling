package com.counseling.api.adapter.inbound.web.dto

import com.counseling.api.domain.CoBrowsingSession
import java.time.Instant

data class CoBrowsingSessionResponse(
    val sessionId: String?,
    val channelId: String,
    val initiatedBy: String,
    val status: String,
    val startedAt: Instant?,
    val endedAt: Instant?,
    val durationSeconds: Long?,
)

data class CoBrowsingSessionListResponse(
    val sessions: List<CoBrowsingSessionResponse>,
    val hasMore: Boolean,
    val oldestTimestamp: Instant?,
)

fun CoBrowsingSession.toResponse(): CoBrowsingSessionResponse {
    val duration =
        if (startedAt != null && endedAt != null) {
            endedAt.epochSecond - startedAt.epochSecond
        } else {
            null
        }
    return CoBrowsingSessionResponse(
        sessionId = id,
        channelId = channelId,
        initiatedBy = initiatedBy,
        status = status.name,
        startedAt = startedAt,
        endedAt = endedAt,
        durationSeconds = duration,
    )
}
