package com.counseling.admin.port.outbound

import com.counseling.admin.domain.Feedback
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface AdminFeedbackRepository {
    fun findById(id: UUID): Mono<Feedback>

    fun findAll(
        agentId: UUID?,
        rating: Int?,
        page: Int,
        size: Int,
    ): Flux<Feedback>

    fun countAll(
        agentId: UUID?,
        rating: Int?,
    ): Mono<Long>
}
