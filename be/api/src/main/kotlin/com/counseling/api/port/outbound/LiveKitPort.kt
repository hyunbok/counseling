package com.counseling.api.port.outbound

import reactor.core.publisher.Mono

interface LiveKitPort {
    fun createRoom(
        roomName: String,
        emptyTimeoutSec: Int = 300,
        maxParticipants: Int = 2,
    ): Mono<String>

    fun deleteRoom(roomName: String): Mono<Void>

    fun generateToken(
        roomName: String,
        identity: String,
        name: String,
        canPublish: Boolean = true,
        canSubscribe: Boolean = true,
    ): String
}
