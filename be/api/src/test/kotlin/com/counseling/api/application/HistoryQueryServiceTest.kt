package com.counseling.api.application

import com.counseling.api.domain.TenantContext
import com.counseling.api.domain.exception.NotFoundException
import com.counseling.api.port.inbound.HistoryFilter
import com.counseling.api.port.outbound.HistoryProjection
import com.counseling.api.port.outbound.HistoryReadRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import reactor.util.context.Context
import java.time.Instant
import java.util.UUID

class HistoryQueryServiceTest :
    StringSpec({
        val historyReadRepository = mockk<HistoryReadRepository>()
        val service = HistoryQueryService(historyReadRepository)

        val tenantId = "test-tenant"

        afterEach { clearAllMocks() }

        fun makeProjection(
            channelId: UUID = UUID.randomUUID(),
            agentId: UUID = UUID.randomUUID(),
            endedAt: Instant? = Instant.now(),
        ): HistoryProjection =
            HistoryProjection(
                channelId = channelId,
                tenantId = tenantId,
                agentId = agentId,
                agentName = "Test Agent",
                groupId = null,
                groupName = null,
                customerName = "Test Customer",
                customerContact = null,
                status = "CLOSED",
                startedAt = Instant.now().minusSeconds(300),
                endedAt = endedAt,
                durationSeconds = 300L,
                recording = null,
                feedback = null,
                counselNote = null,
            )

        "list() should return paginated results" {
            val filter = HistoryFilter(limit = 10)
            val projections = (1..3).map { makeProjection() }

            every {
                historyReadRepository.findByTenantId(tenantId, null, null, null, null, null, 11)
            } returns Mono.just(projections)

            StepVerifier
                .create(
                    service
                        .list(tenantId, filter)
                        .contextWrite(TenantContext.withTenantId(Context.empty(), tenantId)),
                ).assertNext { result ->
                    result.items.size shouldBe 3
                    result.hasMore shouldBe false
                }.verifyComplete()
        }

        "list() should set hasMore when more results exist" {
            val limit = 3
            val filter = HistoryFilter(limit = limit)
            val projections = (1..(limit + 1)).map { makeProjection() }

            every {
                historyReadRepository.findByTenantId(tenantId, null, null, null, null, null, limit + 1)
            } returns Mono.just(projections)

            StepVerifier
                .create(
                    service
                        .list(tenantId, filter)
                        .contextWrite(TenantContext.withTenantId(Context.empty(), tenantId)),
                ).assertNext { result ->
                    result.items.size shouldBe limit
                    result.hasMore shouldBe true
                }.verifyComplete()
        }

        "list() should filter by agentId for COUNSELOR role" {
            val agentId = UUID.randomUUID()
            val filter = HistoryFilter(agentId = agentId, limit = 20)
            val projections = listOf(makeProjection(agentId = agentId))

            every {
                historyReadRepository.findByTenantId(tenantId, agentId, null, null, null, null, 21)
            } returns Mono.just(projections)

            StepVerifier
                .create(
                    service
                        .list(tenantId, filter)
                        .contextWrite(TenantContext.withTenantId(Context.empty(), tenantId)),
                ).assertNext { result ->
                    result.items.size shouldBe 1
                    result.items[0].agentId shouldBe agentId
                }.verifyComplete()
        }

        "getDetail() should return history detail" {
            val channelId = UUID.randomUUID()
            val projection = makeProjection(channelId = channelId)

            every {
                historyReadRepository.findByChannelId(channelId, tenantId)
            } returns Mono.just(projection)

            StepVerifier
                .create(
                    service
                        .getDetail(tenantId, channelId)
                        .contextWrite(TenantContext.withTenantId(Context.empty(), tenantId)),
                ).assertNext { detail ->
                    detail.channelId shouldBe channelId
                    detail.customerName shouldBe "Test Customer"
                    detail.status shouldBe "CLOSED"
                }.verifyComplete()
        }

        "getDetail() should return NotFoundException when not found" {
            val channelId = UUID.randomUUID()

            every {
                historyReadRepository.findByChannelId(channelId, tenantId)
            } returns Mono.empty()

            StepVerifier
                .create(
                    service
                        .getDetail(tenantId, channelId)
                        .contextWrite(TenantContext.withTenantId(Context.empty(), tenantId)),
                ).expectErrorMatches { it is NotFoundException }
                .verify()
        }
    })
