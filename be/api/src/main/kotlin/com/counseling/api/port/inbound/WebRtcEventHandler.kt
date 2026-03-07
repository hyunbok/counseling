package com.counseling.api.port.inbound

import reactor.core.publisher.Mono

interface WebRtcEventHandler {
    fun onRoomFinished(roomName: String): Mono<Void>
}
