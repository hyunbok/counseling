package com.counseling.api.port.outbound

import com.counseling.api.domain.CoBrowsingSession
import reactor.core.publisher.Flux

interface CoBrowsingNotificationPort {
    fun emitSessionUpdate(
        channelId: String,
        session: CoBrowsingSession,
    )

    fun subscribeSessionUpdates(channelId: String): Flux<CoBrowsingSession>

    fun removeChannel(channelId: String)
}
