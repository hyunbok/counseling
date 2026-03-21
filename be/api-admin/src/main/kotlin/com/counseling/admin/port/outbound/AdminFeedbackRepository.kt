package com.counseling.admin.port.outbound

import com.counseling.admin.domain.Feedback
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface AdminFeedbackRepository {
    fun findById(id: String): Mono<Feedback>

    fun findAll(
        agentId: String?,
        rating: Int?,
        page: Int,
        size: Int,
    ): Flux<Feedback>

    fun countAll(
        agentId: String?,
        rating: Int?,
    ): Mono<Long>
}
