package com.counseling.admin.application

import com.counseling.admin.domain.Feedback
import com.counseling.admin.domain.exception.NotFoundException
import com.counseling.admin.port.inbound.FeedbackQuery
import com.counseling.admin.port.outbound.AdminFeedbackRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Service
@Profile("!test")
class FeedbackQueryService(
    private val feedbackRepository: AdminFeedbackRepository,
) : FeedbackQuery {
    override fun listFeedbacks(
        agentId: UUID?,
        rating: Int?,
    ): Flux<Feedback> = feedbackRepository.findAll(agentId, rating)

    override fun getFeedback(id: UUID): Mono<Feedback> =
        feedbackRepository
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException("Feedback not found: $id")))
}
