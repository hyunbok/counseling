package com.counseling.api.port.outbound

import com.counseling.api.domain.CoBrowsingSession
import reactor.core.publisher.Flux
import java.util.UUID

interface CoBrowsingNotificationPort {
    fun emitSessionUpdate(
        channelId: UUID,
        session: CoBrowsingSession,
    )

    fun subscribeSessionUpdates(channelId: UUID): Flux<CoBrowsingSession>

    fun removeChannel(channelId: UUID)
}
