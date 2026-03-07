package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.CustomerDeviceInfo
import com.counseling.api.port.outbound.CounselNoteProjection
import com.counseling.api.port.outbound.FeedbackProjection
import com.counseling.api.port.outbound.HistoryProjection
import com.counseling.api.port.outbound.RecordingProjection
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.util.UUID

@Document(collection = "channel_histories")
@CompoundIndexes(
    CompoundIndex(
        name = "idx_tenant_channel",
        def = "{'tenantId': 1, 'channelId': 1}",
        unique = true,
    ),
    CompoundIndex(
        name = "idx_tenant_ended",
        def = "{'tenantId': 1, 'endedAt': -1}",
    ),
    CompoundIndex(
        name = "idx_tenant_agent_ended",
        def = "{'tenantId': 1, 'agentId': 1, 'endedAt': -1}",
    ),
    CompoundIndex(
        name = "idx_tenant_group_ended",
        def = "{'tenantId': 1, 'groupId': 1, 'endedAt': -1}",
    ),
)
data class ChannelHistoryDocument(
    @Id
    val id: String,
    val channelId: String,
    val tenantId: String,
    val agentId: String?,
    val agentName: String?,
    val groupId: String?,
    val groupName: String?,
    val customerName: String?,
    val customerContact: String?,
    val customerDevice: EmbeddedCustomerDevice?,
    val status: String,
    val startedAt: Instant?,
    val endedAt: Instant?,
    val durationSeconds: Long?,
    val recording: EmbeddedRecording?,
    val feedback: EmbeddedFeedback?,
    val counselNote: EmbeddedCounselNote?,
) {
    fun toProjection(): HistoryProjection =
        HistoryProjection(
            channelId = UUID.fromString(channelId),
            tenantId = tenantId,
            agentId = agentId?.let { UUID.fromString(it) },
            agentName = agentName,
            groupId = groupId?.let { UUID.fromString(it) },
            groupName = groupName,
            customerName = customerName,
            customerContact = customerContact,
            customerDevice = customerDevice?.toInfo(),
            status = status,
            startedAt = startedAt,
            endedAt = endedAt,
            durationSeconds = durationSeconds,
            recording = recording?.toProjection(),
            feedback = feedback?.toProjection(),
            counselNote = counselNote?.toProjection(),
        )

    companion object {
        fun fromProjection(projection: HistoryProjection): ChannelHistoryDocument =
            ChannelHistoryDocument(
                id = projection.channelId.toString(),
                channelId = projection.channelId.toString(),
                tenantId = projection.tenantId,
                agentId = projection.agentId?.toString(),
                agentName = projection.agentName,
                groupId = projection.groupId?.toString(),
                groupName = projection.groupName,
                customerName = projection.customerName,
                customerContact = projection.customerContact,
                customerDevice = projection.customerDevice?.let { EmbeddedCustomerDevice.fromInfo(it) },
                status = projection.status,
                startedAt = projection.startedAt,
                endedAt = projection.endedAt,
                durationSeconds = projection.durationSeconds,
                recording = projection.recording?.let { EmbeddedRecording.fromProjection(it) },
                feedback = projection.feedback?.let { EmbeddedFeedback.fromProjection(it) },
                counselNote = projection.counselNote?.let { EmbeddedCounselNote.fromProjection(it) },
            )
    }
}

data class EmbeddedRecording(
    val recordingId: String,
    val status: String,
    val filePath: String?,
    val startedAt: Instant,
    val stoppedAt: Instant?,
) {
    fun toProjection(): RecordingProjection =
        RecordingProjection(
            recordingId = UUID.fromString(recordingId),
            status = status,
            filePath = filePath,
            startedAt = startedAt,
            stoppedAt = stoppedAt,
        )

    companion object {
        fun fromProjection(projection: RecordingProjection): EmbeddedRecording =
            EmbeddedRecording(
                recordingId = projection.recordingId.toString(),
                status = projection.status,
                filePath = projection.filePath,
                startedAt = projection.startedAt,
                stoppedAt = projection.stoppedAt,
            )
    }
}

data class EmbeddedFeedback(
    val rating: Int,
    val comment: String?,
    val createdAt: Instant,
) {
    fun toProjection(): FeedbackProjection =
        FeedbackProjection(
            rating = rating,
            comment = comment,
            createdAt = createdAt,
        )

    companion object {
        fun fromProjection(projection: FeedbackProjection): EmbeddedFeedback =
            EmbeddedFeedback(
                rating = projection.rating,
                comment = projection.comment,
                createdAt = projection.createdAt,
            )
    }
}

data class EmbeddedCounselNote(
    val noteId: String,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun toProjection(): CounselNoteProjection =
        CounselNoteProjection(
            noteId = UUID.fromString(noteId),
            content = content,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    companion object {
        fun fromProjection(projection: CounselNoteProjection): EmbeddedCounselNote =
            EmbeddedCounselNote(
                noteId = projection.noteId.toString(),
                content = projection.content,
                createdAt = projection.createdAt,
                updatedAt = projection.updatedAt,
            )
    }
}

data class EmbeddedCustomerDevice(
    val deviceType: String?,
    val deviceBrand: String?,
    val osName: String?,
    val osVersion: String?,
    val browserName: String?,
    val browserVersion: String?,
    val rawUserAgent: String?,
) {
    fun toInfo(): CustomerDeviceInfo =
        CustomerDeviceInfo(
            deviceType = deviceType,
            deviceBrand = deviceBrand,
            osName = osName,
            osVersion = osVersion,
            browserName = browserName,
            browserVersion = browserVersion,
            rawUserAgent = rawUserAgent,
        )

    companion object {
        fun fromInfo(info: CustomerDeviceInfo): EmbeddedCustomerDevice =
            EmbeddedCustomerDevice(
                deviceType = info.deviceType,
                deviceBrand = info.deviceBrand,
                osName = info.osName,
                osVersion = info.osVersion,
                browserName = info.browserName,
                browserVersion = info.browserVersion,
                rawUserAgent = info.rawUserAgent,
            )
    }
}
