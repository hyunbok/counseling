package com.counseling.api.adapter.inbound.web.controller

import com.counseling.api.adapter.inbound.web.dto.FeedbackResponse
import com.counseling.api.adapter.inbound.web.dto.FeedbackStatsResponse
import com.counseling.api.adapter.inbound.web.dto.SubmitFeedbackRequest
import com.counseling.api.domain.Feedback
import com.counseling.api.domain.auth.AuthenticatedAgent
import com.counseling.api.domain.exception.BadRequestException
import com.counseling.api.domain.exception.UnauthorizedException
import com.counseling.api.port.inbound.FeedbackQuery
import com.counseling.api.port.inbound.FeedbackUseCase
import com.counseling.api.port.inbound.SubmitFeedbackCommand
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@RestController
@Profile("!test")
class FeedbackController(
    private val feedbackUseCase: FeedbackUseCase,
    private val feedbackQuery: FeedbackQuery,
) {
    @PostMapping("/api/channels/{channelId}/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    fun submitFeedback(
        @PathVariable channelId: UUID,
        @RequestBody request: SubmitFeedbackRequest,
    ): Mono<FeedbackResponse> =
        feedbackUseCase
            .submit(
                SubmitFeedbackCommand(
                    channelId = channelId,
                    rating = request.rating,
                    comment = request.comment,
                ),
            ).map { it.toResponse() }

    @GetMapping("/api/channels/{channelId}/feedback")
    fun getFeedback(
        @PathVariable channelId: UUID,
    ): Mono<FeedbackResponse> =
        authenticatedAgent().flatMap {
            feedbackQuery.getByChannelId(channelId).map { it.toResponse() }
        }

    @GetMapping("/api/feedback/stats")
    fun getStats(
        @RequestParam(required = false) from: Instant?,
        @RequestParam(required = false) to: Instant?,
    ): Mono<FeedbackStatsResponse> =
        authenticatedAgent().flatMap {
            val now = Instant.now()
            val effectiveFrom = from ?: now.minus(30, ChronoUnit.DAYS)
            val effectiveTo = to ?: now
            if (effectiveFrom.isAfter(effectiveTo)) {
                return@flatMap Mono.error<FeedbackStatsResponse>(
                    BadRequestException("'from' must be before 'to'"),
                )
            }
            feedbackQuery.getStats(effectiveFrom, effectiveTo).map { result ->
                FeedbackStatsResponse(
                    totalCount = result.totalCount,
                    averageRating = result.averageRating,
                    distribution = result.distribution,
                    from = result.from,
                    to = result.to,
                )
            }
        }

    private fun authenticatedAgent(): Mono<AuthenticatedAgent> =
        ReactiveSecurityContextHolder
            .getContext()
            .flatMap { ctx ->
                val principal = ctx.authentication?.principal
                if (principal is AuthenticatedAgent) {
                    Mono.just(principal)
                } else {
                    Mono.error(UnauthorizedException("Not authenticated"))
                }
            }

    private fun Feedback.toResponse(): FeedbackResponse =
        FeedbackResponse(
            id = id,
            channelId = channelId,
            rating = rating,
            comment = comment,
            createdAt = createdAt,
        )
}
