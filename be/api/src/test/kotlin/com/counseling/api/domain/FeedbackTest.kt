package com.counseling.api.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.util.UUID

class FeedbackTest :
    StringSpec({
        val baseInstant = Instant.parse("2026-01-01T00:00:00Z")

        fun createFeedback(rating: Int): Feedback =
            Feedback(
                id = UUID.randomUUID(),
                channelId = UUID.randomUUID(),
                rating = rating,
                comment = "test comment",
                createdAt = baseInstant,
            )

        "rating 1 is valid" {
            val feedback = createFeedback(1)
            feedback.rating shouldBe 1
        }

        "rating 5 is valid" {
            val feedback = createFeedback(5)
            feedback.rating shouldBe 5
        }

        "rating 3 is valid" {
            val feedback = createFeedback(3)
            feedback.rating shouldBe 3
        }

        "rating 0 throws IllegalArgumentException" {
            shouldThrow<IllegalArgumentException> {
                createFeedback(0)
            }
        }

        "rating 6 throws IllegalArgumentException" {
            shouldThrow<IllegalArgumentException> {
                createFeedback(6)
            }
        }

        "negative rating throws IllegalArgumentException" {
            shouldThrow<IllegalArgumentException> {
                createFeedback(-1)
            }
        }
    })
