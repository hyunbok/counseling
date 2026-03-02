package com.counseling.api.domain

import java.time.Instant
import java.util.UUID

data class Recording(
    val id: UUID,
    val channelId: UUID,
    val egressId: String,
    val status: RecordingStatus,
    val filePath: String?,
    val startedAt: Instant,
    val stoppedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deleted: Boolean = false,
) {
    fun stop(filePath: String?): Recording =
        copy(
            status = RecordingStatus.STOPPED,
            filePath = filePath,
            stoppedAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    fun markFailed(): Recording =
        copy(
            status = RecordingStatus.FAILED,
            updatedAt = Instant.now(),
        )

    fun isActive(): Boolean = status == RecordingStatus.RECORDING
}
