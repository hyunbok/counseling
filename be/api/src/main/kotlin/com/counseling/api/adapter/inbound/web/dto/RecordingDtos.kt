package com.counseling.api.adapter.inbound.web.dto

import java.time.Instant

data class StartRecordingResponse(
    val recordingId: String,
    val channelId: String,
    val egressId: String,
    val status: String,
    val startedAt: Instant,
)

data class StopRecordingResponse(
    val recordingId: String,
    val channelId: String,
    val egressId: String,
    val status: String,
    val startedAt: Instant,
    val stoppedAt: Instant?,
    val filePath: String?,
)

data class RecordingInfoResponse(
    val recordingId: String,
    val channelId: String,
    val egressId: String,
    val status: String,
    val startedAt: Instant,
    val stoppedAt: Instant?,
    val filePath: String?,
)

data class RecordingListResponse(
    val recordings: List<RecordingInfoResponse>,
)
