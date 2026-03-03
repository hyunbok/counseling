package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.ScreenCapture
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.util.UUID

@Document(collection = "screen_captures")
@CompoundIndex(name = "idx_tenant_channel_created", def = "{'tenantId': 1, 'channelId': 1, 'createdAt': -1}")
data class ScreenCaptureDocument(
    @Id
    val id: String,
    val tenantId: String?,
    val channelId: String,
    val capturedBy: String,
    val originalFilename: String,
    val contentType: String,
    val fileSize: Long,
    val note: String?,
    val createdAt: Instant,
    val deleted: Boolean = false,
) {
    fun toDomain(): ScreenCapture =
        ScreenCapture(
            id = UUID.fromString(id),
            channelId = UUID.fromString(channelId),
            capturedBy = UUID.fromString(capturedBy),
            originalFilename = originalFilename,
            storedFilename = "",
            contentType = contentType,
            fileSize = fileSize,
            storagePath = "",
            note = note,
            createdAt = createdAt,
            deleted = deleted,
        )

    companion object {
        fun fromDomain(
            capture: ScreenCapture,
            tenantId: String? = null,
        ): ScreenCaptureDocument =
            ScreenCaptureDocument(
                id = capture.id.toString(),
                tenantId = tenantId,
                channelId = capture.channelId.toString(),
                capturedBy = capture.capturedBy.toString(),
                originalFilename = capture.originalFilename,
                contentType = capture.contentType,
                fileSize = capture.fileSize,
                note = capture.note,
                createdAt = capture.createdAt,
                deleted = capture.deleted,
            )
    }
}
