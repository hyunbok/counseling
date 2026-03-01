package com.counseling.api.domain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.util.UUID

class TenantTest :
    StringSpec({
        val baseInstant = Instant.parse("2026-01-01T00:00:00Z")

        fun createTenant(status: TenantStatus = TenantStatus.PENDING): Tenant =
            Tenant(
                id = UUID.randomUUID(),
                name = "Test Corp",
                slug = "test-corp",
                status = status,
                dbHost = "localhost",
                dbPort = 5432,
                dbName = "test_db",
                dbUsername = "user",
                dbPassword = "pass",
                createdAt = baseInstant,
                updatedAt = baseInstant,
                deleted = false,
            )

        "activate() changes status to ACTIVE" {
            val tenant = createTenant(TenantStatus.PENDING)
            val activated = tenant.activate()
            activated.status shouldBe TenantStatus.ACTIVE
        }

        "suspend() changes status to SUSPENDED" {
            val tenant = createTenant(TenantStatus.ACTIVE)
            val suspended = tenant.suspend()
            suspended.status shouldBe TenantStatus.SUSPENDED
        }

        "deactivate() changes status to DEACTIVATED" {
            val tenant = createTenant(TenantStatus.ACTIVE)
            val deactivated = tenant.deactivate()
            deactivated.status shouldBe TenantStatus.DEACTIVATED
        }

        "softDelete() sets deleted to true" {
            val tenant = createTenant(TenantStatus.ACTIVE)
            val deleted = tenant.softDelete()
            deleted.deleted shouldBe true
        }

        "softDelete() does not change status" {
            val tenant = createTenant(TenantStatus.ACTIVE)
            val deleted = tenant.softDelete()
            deleted.status shouldBe TenantStatus.ACTIVE
        }

        "isRoutable() returns true for ACTIVE and not deleted" {
            val tenant = createTenant(TenantStatus.ACTIVE)
            tenant.isRoutable() shouldBe true
        }

        "isRoutable() returns false for PENDING" {
            val tenant = createTenant(TenantStatus.PENDING)
            tenant.isRoutable() shouldBe false
        }

        "isRoutable() returns false for SUSPENDED" {
            val tenant = createTenant(TenantStatus.SUSPENDED)
            tenant.isRoutable() shouldBe false
        }

        "isRoutable() returns false for DEACTIVATED" {
            val tenant = createTenant(TenantStatus.DEACTIVATED)
            tenant.isRoutable() shouldBe false
        }

        "isRoutable() returns false when deleted even if ACTIVE" {
            val tenant = createTenant(TenantStatus.ACTIVE).copy(deleted = true)
            tenant.isRoutable() shouldBe false
        }

        "updateConnectionInfo() updates connection fields" {
            val tenant = createTenant(TenantStatus.ACTIVE)
            val updated = tenant.updateConnectionInfo("new-host", 5433, "new_db", "new_user", "new_pass")
            updated.dbHost shouldBe "new-host"
            updated.dbPort shouldBe 5433
            updated.dbName shouldBe "new_db"
            updated.dbUsername shouldBe "new_user"
            updated.dbPassword shouldBe "new_pass"
        }

        "state transitions return new instances (immutability)" {
            val tenant = createTenant(TenantStatus.PENDING)
            val activated = tenant.activate()
            tenant.status shouldBe TenantStatus.PENDING
            activated.status shouldBe TenantStatus.ACTIVE
        }
    })
