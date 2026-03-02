package com.counseling.api.adapter.inbound.web.dto

import java.time.Instant
import java.util.UUID

data class StartRecordingResponse(
    val recordingId: UUID,
    val channelId: UUID,
    val egressId: String,
    val status: String,
    val startedAt: Instant,
)

data class StopRecordingResponse(
    val recordingId: UUID,
    val channelId: UUID,
    val egressId: String,
    val status: String,
    val startedAt: Instant,
    val stoppedAt: Instant?,
    val filePath: String?,
)

data class RecordingInfoResponse(
    val recordingId: UUID,
    val channelId: UUID,
    val egressId: String,
    val status: String,
    val startedAt: Instant,
    val stoppedAt: Instant?,
    val filePath: String?,
)

data class RecordingListResponse(
    val recordings: List<RecordingInfoResponse>,
)
