package com.counseling.api.domain

import java.time.Instant
import java.util.UUID

data class Endpoint(
    val id: UUID,
    val channelId: UUID,
    val type: EndpointType,
    val customerName: String?,
    val customerContact: String?,
    val joinedAt: Instant,
    val leftAt: Instant?,
) {
    fun leave(): Endpoint = copy(leftAt = Instant.now())
}
