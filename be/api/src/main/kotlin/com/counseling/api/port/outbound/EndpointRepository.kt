package com.counseling.api.port.outbound

import com.counseling.api.domain.Endpoint
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface EndpointRepository {
    fun save(endpoint: Endpoint): Mono<Endpoint>

    fun findAllByChannelId(channelId: UUID): Flux<Endpoint>
}
