package com.counseling.api.application

import com.counseling.api.domain.Feedback
import com.counseling.api.domain.FeedbackStatsResult
import com.counseling.api.domain.TenantContext
import com.counseling.api.port.outbound.FeedbackReadRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class FeedbackQueryServiceTest :
    StringSpec({
        val feedbackReadRepository = mockk<FeedbackReadRepository>()

        val queryService = FeedbackQueryService(feedbackReadRepository)

        val tenantId = "test-tenant"

        afterEach { clearAllMocks() }

        fun makeFeedback(): Feedback =
            Feedback(
                id = UUID.randomUUID(),
                channelId = UUID.randomUUID(),
                rating = 4,
                comment = "Good",
                createdAt = Instant.now(),
            )

        fun makeStats(): FeedbackStatsResult {
            val now = Instant.now()
            return FeedbackStatsResult(
                totalCount = 10,
                averageRating = 4.2,
                distribution = mapOf(1 to 0L, 2 to 1L, 3 to 2L, 4 to 3L, 5 to 4L),
                from = now.minus(30, ChronoUnit.DAYS),
                to = now,
            )
        }

        "getByChannelId() should return feedback from read store" {
            val feedback = makeFeedback()

            every {
                feedbackReadRepository.findByChannelId(feedback.channelId, tenantId)
            } returns Mono.just(feedback)

            StepVerifier
                .create(
                    queryService
                        .getByChannelId(feedback.channelId)
                        .contextWrite(
                            TenantContext.withTenantId(
                                reactor.util.context.Context
                                    .empty(),
                                tenantId,
                            ),
                        ),
                ).assertNext { result ->
                    result.id shouldBe feedback.id
                    result.channelId shouldBe feedback.channelId
                    result.rating shouldBe feedback.rating
                    result.comment shouldBe feedback.comment
                }.verifyComplete()
        }

        "getByChannelId() should return empty when feedback not found" {
            val channelId = UUID.randomUUID()

            every {
                feedbackReadRepository.findByChannelId(channelId, tenantId)
            } returns Mono.empty()

            StepVerifier
                .create(
                    queryService
                        .getByChannelId(channelId)
                        .contextWrite(
                            TenantContext.withTenantId(
                                reactor.util.context.Context
                                    .empty(),
                                tenantId,
                            ),
                        ),
                ).verifyComplete()
        }

        "getStats() should return statistics from read store" {
            val stats = makeStats()
            val from = stats.from
            val to = stats.to

            every {
                feedbackReadRepository.getStats(tenantId, from, to)
            } returns Mono.just(stats)

            StepVerifier
                .create(
                    queryService
                        .getStats(from, to)
                        .contextWrite(
                            TenantContext.withTenantId(
                                reactor.util.context.Context
                                    .empty(),
                                tenantId,
                            ),
                        ),
                ).assertNext { result ->
                    result.totalCount shouldBe 10L
                    result.averageRating shouldBe 4.2
                    result.distribution shouldBe mapOf(1 to 0L, 2 to 1L, 3 to 2L, 4 to 3L, 5 to 4L)
                    result.from shouldBe from
                    result.to shouldBe to
                }.verifyComplete()
        }
    })
