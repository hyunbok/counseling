package com.counseling.api.adapter.inbound.web.controller

import com.counseling.api.domain.exception.UnauthorizedException
import com.counseling.api.port.inbound.HistoryDetail
import com.counseling.api.port.inbound.HistoryFilter
import com.counseling.api.port.inbound.HistoryListItem
import com.counseling.api.port.inbound.HistoryListResult
import com.counseling.api.port.inbound.HistoryQuery
import com.counseling.api.port.inbound.RecordingStreamUseCase
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.util.UUID

class HistoryControllerTest :
    StringSpec({
        val historyQuery = mockk<HistoryQuery>()
        val recordingStreamUseCase = mockk<RecordingStreamUseCase>()

        val tenantId = "test-tenant"
        val agentId = UUID.randomUUID()
        val channelId = UUID.randomUUID()
        val now = Instant.now()

        fun makeListItem(
            id: UUID = UUID.randomUUID(),
            itemAgentId: UUID = agentId,
        ): HistoryListItem =
            HistoryListItem(
                channelId = id,
                agentId = itemAgentId,
                agentName = "Test Agent",
                groupId = null,
                groupName = null,
                customerName = "Customer",
                status = "CLOSED",
                startedAt = now.minusSeconds(300),
                endedAt = now,
                durationSeconds = 300L,
                hasRecording = false,
                hasFeedback = false,
                feedbackRating = null,
            )

        fun makeDetail(detailAgentId: UUID = agentId): HistoryDetail =
            HistoryDetail(
                channelId = channelId,
                agentId = detailAgentId,
                agentName = "Test Agent",
                groupId = null,
                groupName = null,
                customerName = "Customer",
                customerContact = null,
                status = "CLOSED",
                startedAt = now.minusSeconds(300),
                endedAt = now,
                durationSeconds = 300L,
                recording = null,
                feedback = null,
                counselNote = null,
            )

        "list() use case returns paginated HistoryListResult" {
            val filter = HistoryFilter(agentId = agentId, limit = 20)
            val items = listOf(makeListItem())
            val result = HistoryListResult(items = items, hasMore = false)

            every { historyQuery.list(tenantId, filter) } returns Mono.just(result)

            StepVerifier
                .create(historyQuery.list(tenantId, filter))
                .assertNext { queryResult ->
                    queryResult.items.size shouldBe 1
                    queryResult.hasMore shouldBe false
                }.verifyComplete()
        }

        "list() use case sets hasMore when more results exist" {
            val filter = HistoryFilter(agentId = agentId, limit = 2)
            val items = (1..2).map { makeListItem(id = UUID.randomUUID()) }
            val result = HistoryListResult(items = items, hasMore = true)

            every { historyQuery.list(tenantId, filter) } returns Mono.just(result)

            StepVerifier
                .create(historyQuery.list(tenantId, filter))
                .assertNext { queryResult ->
                    queryResult.items.size shouldBe 2
                    queryResult.hasMore shouldBe true
                }.verifyComplete()
        }

        "getDetail() use case returns HistoryDetail" {
            val detail = makeDetail()

            every { historyQuery.getDetail(tenantId, channelId) } returns Mono.just(detail)

            StepVerifier
                .create(historyQuery.getDetail(tenantId, channelId))
                .assertNext { d ->
                    d.channelId shouldBe channelId
                    d.agentId shouldBe agentId
                    d.status shouldBe "CLOSED"
                }.verifyComplete()
        }

        "getDetail() use case propagates NotFoundException for missing channel" {
            every { historyQuery.getDetail(tenantId, channelId) } returns
                Mono.error(UnauthorizedException("Not found"))

            StepVerifier
                .create(historyQuery.getDetail(tenantId, channelId))
                .expectErrorMatches { it is UnauthorizedException }
                .verify()
        }

        "COUNSELOR role should only see own records" {
            val counselorAgentId = UUID.randomUUID()
            val otherAgentId = UUID.randomUUID()
            val filterForCounselor = HistoryFilter(agentId = counselorAgentId, limit = 20)
            val items = listOf(makeListItem(itemAgentId = counselorAgentId))
            val result = HistoryListResult(items = items, hasMore = false)

            every { historyQuery.list(tenantId, filterForCounselor) } returns Mono.just(result)

            StepVerifier
                .create(historyQuery.list(tenantId, filterForCounselor))
                .assertNext { queryResult ->
                    queryResult.items.size shouldBe 1
                    queryResult.items[0].agentId shouldBe counselorAgentId
                }.verifyComplete()
        }

        "ADMIN role can list all records" {
            val adminFilter = HistoryFilter(limit = 20)
            val items = (1..3).map { makeListItem(itemAgentId = UUID.randomUUID()) }
            val result = HistoryListResult(items = items, hasMore = false)

            every { historyQuery.list(tenantId, adminFilter) } returns Mono.just(result)

            StepVerifier
                .create(historyQuery.list(tenantId, adminFilter))
                .assertNext { queryResult ->
                    queryResult.items.size shouldBe 3
                    queryResult.hasMore shouldBe false
                }.verifyComplete()
        }
    })
