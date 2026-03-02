package com.counseling.admin.adapter.inbound.web.controller

import com.counseling.admin.adapter.inbound.web.dto.FeedbackResponse
import com.counseling.admin.port.inbound.FeedbackQuery
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/api-adm/feedbacks")
@Profile("!test")
class FeedbackController(
    private val feedbackQuery: FeedbackQuery,
) {
    @GetMapping
    fun listFeedbacks(
        @RequestParam(required = false) agentId: UUID?,
        @RequestParam(required = false) rating: Int?,
    ): Flux<FeedbackResponse> =
        feedbackQuery.listFeedbacks(agentId, rating).map { feedback ->
            FeedbackResponse(
                id = feedback.id,
                channelId = feedback.channelId,
                rating = feedback.rating,
                comment = feedback.comment,
                createdAt = feedback.createdAt,
            )
        }

    @GetMapping("/{id}")
    fun getFeedback(
        @PathVariable id: UUID,
    ): Mono<FeedbackResponse> =
        feedbackQuery.getFeedback(id).map { feedback ->
            FeedbackResponse(
                id = feedback.id,
                channelId = feedback.channelId,
                rating = feedback.rating,
                comment = feedback.comment,
                createdAt = feedback.createdAt,
            )
        }
}
