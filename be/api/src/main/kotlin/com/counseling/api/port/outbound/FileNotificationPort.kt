package com.counseling.api.port.outbound

import com.counseling.api.domain.SharedFile
import reactor.core.publisher.Flux
import java.util.UUID

interface FileNotificationPort {
    fun emitFile(
        channelId: UUID,
        file: SharedFile,
    )

    fun subscribeFiles(channelId: UUID): Flux<SharedFile>

    fun removeChannel(channelId: UUID)
}
