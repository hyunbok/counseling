package com.counseling.api.application

import com.counseling.api.adapter.outbound.external.InMemoryQueueNotificationAdapter
import com.counseling.api.domain.PositionUpdate
import com.counseling.api.domain.QueueEntry
import com.counseling.api.domain.QueueUpdate
import com.counseling.api.domain.QueueUpdateType
import io.kotest.core.spec.style.StringSpec
import reactor.test.StepVerifier
import java.time.Instant
import java.util.UUID

class QueueNotificationTest :
    StringSpec({
        val tenantId = "tenant-notify-test"

        fun makeEntry(): QueueEntry =
            QueueEntry(
                id = UUID.randomUUID(),
                customerName = "Test",
                customerContact = "010-0000-0000",
                groupId = null,
                enteredAt = Instant.now(),
            )

        "subscribeAgentUpdates receives emitted QueueUpdate" {
            val adapter = InMemoryQueueNotificationAdapter()
            val entry = makeEntry()
            val update =
                QueueUpdate(
                    type = QueueUpdateType.ENTERED,
                    entry = entry,
                    queueSize = 1L,
                )

            val flux = adapter.subscribeAgentUpdates(tenantId)

            StepVerifier
                .create(flux.take(1))
                .then { adapter.emitQueueUpdate(tenantId, update) }
                .expectNext(update)
                .verifyComplete()
        }

        "subscribePositionUpdates receives emitted PositionUpdate" {
            val adapter = InMemoryQueueNotificationAdapter()
            val entryId = UUID.randomUUID()
            val positionUpdate =
                PositionUpdate(
                    entryId = entryId,
                    position = 2L,
                    queueSize = 5L,
                )

            val flux = adapter.subscribePositionUpdates(tenantId)

            StepVerifier
                .create(flux.take(1))
                .then { adapter.emitPositionUpdate(tenantId, positionUpdate) }
                .expectNext(positionUpdate)
                .verifyComplete()
        }

        "each adapter instance has independent sinks per tenant" {
            val adapter1 = InMemoryQueueNotificationAdapter()
            val adapter2 = InMemoryQueueNotificationAdapter()
            val entry = makeEntry()
            val update =
                QueueUpdate(
                    type = QueueUpdateType.LEFT,
                    entry = entry,
                    queueSize = 0L,
                )

            StepVerifier
                .create(adapter1.subscribeAgentUpdates(tenantId).take(1))
                .then { adapter1.emitQueueUpdate(tenantId, update) }
                .expectNext(update)
                .verifyComplete()

            StepVerifier
                .create(adapter2.subscribeAgentUpdates(tenantId).take(1))
                .then { adapter2.emitQueueUpdate(tenantId, update) }
                .expectNext(update)
                .verifyComplete()
        }

        "different tenants have isolated update streams" {
            val adapter = InMemoryQueueNotificationAdapter()
            val tenantA = "tenant-a"
            val tenantB = "tenant-b"
            val entry = makeEntry()
            val updateA =
                QueueUpdate(
                    type = QueueUpdateType.ENTERED,
                    entry = entry,
                    queueSize = 1L,
                )
            val updateB =
                QueueUpdate(
                    type = QueueUpdateType.ACCEPTED,
                    entry = entry,
                    queueSize = 0L,
                )

            val fluxA = adapter.subscribeAgentUpdates(tenantA).take(1)

            StepVerifier
                .create(fluxA)
                .then {
                    adapter.emitQueueUpdate(tenantB, updateB)
                    adapter.emitQueueUpdate(tenantA, updateA)
                }.expectNext(updateA)
                .verifyComplete()
        }
    })
