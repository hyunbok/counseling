package com.counseling.admin.adapter.inbound.web.controller

import com.counseling.admin.adapter.inbound.web.dto.FeedbackResponse
import com.counseling.admin.adapter.inbound.web.dto.PageResponse
import com.counseling.admin.port.inbound.FeedbackQuery
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID
import kotlin.math.ceil

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
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): Mono<PageResponse<FeedbackResponse>> =
        feedbackQuery.listFeedbacks(agentId, rating, page, size).map { result ->
            PageResponse(
                content =
                    result.content.map { feedback ->
                        FeedbackResponse(
                            id = feedback.id,
                            channelId = feedback.channelId,
                            rating = feedback.rating,
                            comment = feedback.comment,
                            createdAt = feedback.createdAt,
                        )
                    },
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = ceil(result.totalElements.toDouble() / result.size).toInt(),
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
