package com.counseling.api.port.outbound

import com.counseling.api.domain.SharedFile
import reactor.core.publisher.Flux

interface FileNotificationPort {
    fun emitFile(
        channelId: String,
        file: SharedFile,
    )

    fun subscribeFiles(channelId: String): Flux<SharedFile>

    fun removeChannel(channelId: String)
}
