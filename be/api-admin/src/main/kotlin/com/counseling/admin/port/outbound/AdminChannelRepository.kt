package com.counseling.admin.port.outbound

import com.counseling.admin.domain.Channel
import reactor.core.publisher.Flux

interface AdminChannelRepository {
    fun findAllActiveChannels(): Flux<Channel>
}
