package com.counseling.api.domain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.util.UUID

class GroupTest :
    StringSpec({
        val baseInstant = Instant.parse("2026-01-01T00:00:00Z")

        fun createGroup(status: GroupStatus = GroupStatus.ACTIVE): Group =
            Group(
                id = UUID.randomUUID(),
                name = "Test Group",
                status = status,
                createdAt = baseInstant,
                updatedAt = baseInstant,
                deleted = false,
            )

        "rename() changes name" {
            val group = createGroup()
            val renamed = group.rename("New Name")
            renamed.name shouldBe "New Name"
        }

        "rename() returns a new instance (immutability)" {
            val group = createGroup()
            val renamed = group.rename("New Name")
            group.name shouldBe "Test Group"
            renamed.name shouldBe "New Name"
        }

        "activate() changes status to ACTIVE" {
            val group = createGroup(GroupStatus.INACTIVE)
            val activated = group.activate()
            activated.status shouldBe GroupStatus.ACTIVE
        }

        "deactivate() changes status to INACTIVE" {
            val group = createGroup(GroupStatus.ACTIVE)
            val deactivated = group.deactivate()
            deactivated.status shouldBe GroupStatus.INACTIVE
        }

        "softDelete() sets deleted to true" {
            val group = createGroup()
            val deleted = group.softDelete()
            deleted.deleted shouldBe true
        }

        "softDelete() does not change status" {
            val group = createGroup(GroupStatus.ACTIVE)
            val deleted = group.softDelete()
            deleted.status shouldBe GroupStatus.ACTIVE
        }
    })
