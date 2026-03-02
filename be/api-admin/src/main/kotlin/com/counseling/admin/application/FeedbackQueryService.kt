package com.counseling.admin.application

import com.counseling.admin.domain.Feedback
import com.counseling.admin.domain.exception.NotFoundException
import com.counseling.admin.port.inbound.FeedbackQuery
import com.counseling.admin.port.inbound.PagedResult
import com.counseling.admin.port.outbound.AdminFeedbackRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import java.util.UUID

@Service
@Profile("!test")
class FeedbackQueryService(
    private val feedbackRepository: AdminFeedbackRepository,
) : FeedbackQuery {
    override fun listFeedbacks(
        agentId: UUID?,
        rating: Int?,
        page: Int,
        size: Int,
    ): Mono<PagedResult<Feedback>> =
        Mono
            .zip(
                feedbackRepository.findAll(agentId, rating, page, size).collectList(),
                feedbackRepository.countAll(agentId, rating),
            ).map { (content, total) -> PagedResult(content, total, page, size) }

    override fun getFeedback(id: UUID): Mono<Feedback> =
        feedbackRepository
            .findById(id)
            .switchIfEmpty(Mono.error(NotFoundException("Feedback not found: $id")))
}
