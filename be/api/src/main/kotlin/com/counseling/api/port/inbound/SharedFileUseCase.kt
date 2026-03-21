package com.counseling.api.port.inbound

import com.counseling.api.domain.SenderType
import com.counseling.api.domain.SharedFile
import org.springframework.core.io.Resource
import reactor.core.publisher.Mono

data class UploadFileCommand(
    val channelId: String,
    val uploaderId: String,
    val uploaderType: SenderType,
    val originalFilename: String,
    val contentType: String,
    val fileSize: Long,
    val content: ByteArray,
)

data class SharedFileResource(
    val resource: Resource,
    val filename: String,
    val contentType: String,
    val contentLength: Long,
)

interface SharedFileUseCase {
    fun upload(command: UploadFileCommand): Mono<SharedFile>

    fun download(
        channelId: String,
        fileId: String,
    ): Mono<SharedFileResource>

    fun delete(
        channelId: String,
        fileId: String,
    ): Mono<Void>
}
