package com.counseling.api.domain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.Instant
import java.util.UUID

class QueueEntryTest :
    StringSpec({
        val fixedTime = Instant.parse("2026-01-01T00:00:00Z")

        fun createEntry(
            groupId: UUID? = null,
            enteredAt: Instant = fixedTime,
        ): QueueEntry =
            QueueEntry(
                id = UUID.randomUUID(),
                customerName = "John Doe",
                customerContact = "010-1234-5678",
                groupId = groupId,
                enteredAt = enteredAt,
            )

        "QueueEntry is created with provided values" {
            val id = UUID.randomUUID()
            val groupId = UUID.randomUUID()
            val entry =
                QueueEntry(
                    id = id,
                    customerName = "Jane Doe",
                    customerContact = "010-9876-5432",
                    groupId = groupId,
                    enteredAt = fixedTime,
                )
            entry.id shouldBe id
            entry.customerName shouldBe "Jane Doe"
            entry.customerContact shouldBe "010-9876-5432"
            entry.groupId shouldBe groupId
            entry.enteredAt shouldBe fixedTime
        }

        "QueueEntry can have null groupId" {
            val entry = createEntry(groupId = null)
            entry.groupId shouldBe null
        }

        "QueueEntry with groupId stores it correctly" {
            val groupId = UUID.randomUUID()
            val entry = createEntry(groupId = groupId)
            entry.groupId shouldBe groupId
        }

        "QueueEntry id is unique for each instance" {
            val entry1 = createEntry()
            val entry2 = createEntry()
            entry1.id shouldNotBe entry2.id
        }

        "QueueEntry is a value object (copy produces equal content)" {
            val entry = createEntry()
            val copy = entry.copy()
            copy shouldBe entry
        }
    })
