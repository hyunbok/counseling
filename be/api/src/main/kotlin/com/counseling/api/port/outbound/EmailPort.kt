package com.counseling.api.port.outbound

import reactor.core.publisher.Mono

data class EmailMessage(
    val to: String,
    val subject: String,
    val htmlBody: String,
)

interface EmailPort {
    fun send(message: EmailMessage): Mono<Void>
}
