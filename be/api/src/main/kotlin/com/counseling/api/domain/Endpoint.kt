package com.counseling.api.domain

import java.time.Instant

data class Endpoint(
    val id: String? = null,
    val channelId: String,
    val type: EndpointType,
    val customerName: String?,
    val customerContact: String?,
    val joinedAt: Instant,
    val leftAt: Instant?,
) {
    fun leave(): Endpoint = copy(leftAt = Instant.now())
}
