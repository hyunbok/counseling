package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.SenderType
import com.counseling.api.domain.SharedFile
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.util.UUID

@Document(collection = "shared_files")
@CompoundIndex(name = "idx_tenant_channel_created", def = "{'tenantId': 1, 'channelId': 1, 'createdAt': -1}")
data class SharedFileDocument(
    @Id
    val id: String,
    val tenantId: String?,
    val channelId: String,
    val uploaderId: String,
    val uploaderType: String,
    val originalFilename: String,
    val contentType: String,
    val fileSize: Long,
    val createdAt: Instant,
    val deleted: Boolean = false,
) {
    fun toDomain(): SharedFile =
        SharedFile(
            id = UUID.fromString(id),
            channelId = UUID.fromString(channelId),
            uploaderId = uploaderId,
            uploaderType = SenderType.valueOf(uploaderType),
            originalFilename = originalFilename,
            storedFilename = "",
            contentType = contentType,
            fileSize = fileSize,
            storagePath = "",
            createdAt = createdAt,
            deleted = deleted,
        )

    companion object {
        fun fromDomain(file: SharedFile): SharedFileDocument =
            SharedFileDocument(
                id = file.id.toString(),
                tenantId = null,
                channelId = file.channelId.toString(),
                uploaderId = file.uploaderId,
                uploaderType = file.uploaderType.name,
                originalFilename = file.originalFilename,
                contentType = file.contentType,
                fileSize = file.fileSize,
                createdAt = file.createdAt,
                deleted = file.deleted,
            )
    }
}
