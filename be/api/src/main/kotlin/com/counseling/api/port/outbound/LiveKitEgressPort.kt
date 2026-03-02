package com.counseling.api.port.outbound

import reactor.core.publisher.Mono

data class EgressStartResult(
    val egressId: String,
)

interface LiveKitEgressPort {
    fun startRoomCompositeEgress(
        roomName: String,
        filePath: String,
    ): Mono<EgressStartResult>

    fun stopEgress(egressId: String): Mono<Void>
}
