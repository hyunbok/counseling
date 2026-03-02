package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.DeliveryMethod
import com.counseling.api.domain.Notification
import com.counseling.api.domain.NotificationType
import com.counseling.api.domain.RecipientType
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import reactor.test.StepVerifier
import java.time.Instant
import java.util.UUID

class NotificationSseAdapterTest :
    StringSpec({
        fun makeNotification(recipientId: UUID = UUID.randomUUID()): Notification =
            Notification(
                id = UUID.randomUUID(),
                recipientId = recipientId,
                recipientType = RecipientType.AGENT,
                type = NotificationType.NEW_COUNSELING_REQUEST,
                title = "Test",
                body = "Body",
                referenceId = null,
                referenceType = null,
                deliveryMethod = DeliveryMethod.IN_APP,
                read = false,
                createdAt = Instant.now(),
            )

        "emit() then subscribe() should receive notification" {
            val adapter = NotificationSseAdapter()
            val recipientId = UUID.randomUUID()
            val notification = makeNotification(recipientId)

            val flux = adapter.subscribe(recipientId)

            StepVerifier
                .create(flux)
                .then { adapter.emit(recipientId, notification) }
                .assertNext { received ->
                    received shouldBe notification
                }.thenCancel()
                .verify()
        }

        "subscribe() from different recipients should be isolated" {
            val adapter = NotificationSseAdapter()
            val recipientA = UUID.randomUUID()
            val recipientB = UUID.randomUUID()
            val notificationA = makeNotification(recipientA)

            val fluxB = adapter.subscribe(recipientB)

            StepVerifier
                .create(fluxB)
                .then { adapter.emit(recipientA, notificationA) }
                .thenCancel()
                .verify()

            val fluxA = adapter.subscribe(recipientA)
            val notificationA2 = makeNotification(recipientA)

            StepVerifier
                .create(fluxA)
                .then { adapter.emit(recipientA, notificationA2) }
                .assertNext { received ->
                    received.recipientId shouldBe recipientA
                }.thenCancel()
                .verify()
        }

        "removeRecipient() should clean up sink" {
            val adapter = NotificationSseAdapter()
            val recipientId = UUID.randomUUID()
            val notification = makeNotification(recipientId)

            val flux = adapter.subscribe(recipientId)

            StepVerifier
                .create(flux)
                .then { adapter.removeRecipient(recipientId) }
                .verifyComplete()

            // After removal, emit should not fail
            adapter.emit(recipientId, notification)
        }
    })
