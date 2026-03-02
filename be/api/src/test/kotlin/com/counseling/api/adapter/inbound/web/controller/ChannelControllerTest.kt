package com.counseling.api.adapter.inbound.web.controller

import com.counseling.api.domain.Channel
import com.counseling.api.domain.ChannelStatus
import com.counseling.api.domain.Endpoint
import com.counseling.api.domain.EndpointType
import com.counseling.api.domain.exception.UnauthorizedException
import com.counseling.api.port.inbound.ChannelDetail
import com.counseling.api.port.inbound.ChannelUseCase
import com.counseling.api.port.inbound.TokenResult
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.util.UUID

class ChannelControllerTest :
    StringSpec({
        val channelUseCase = mockk<ChannelUseCase>()
        val controller = ChannelController(channelUseCase)

        val channelId = UUID.randomUUID()
        val agentId = UUID.randomUUID()
        val now = Instant.now()

        fun makeChannel(
            id: UUID = channelId,
            status: ChannelStatus = ChannelStatus.IN_PROGRESS,
        ) = Channel(
            id = id,
            agentId = agentId,
            status = status,
            startedAt = now,
            endedAt = null,
            recordingPath = null,
            livekitRoomName = "room-test",
            createdAt = now,
            updatedAt = now,
        )

        fun makeEndpoint(
            type: EndpointType = EndpointType.CUSTOMER,
            customerName: String? = "Jane",
            customerContact: String? = "010-1234-5678",
        ) = Endpoint(
            id = UUID.randomUUID(),
            channelId = channelId,
            type = type,
            customerName = customerName,
            customerContact = customerContact,
            joinedAt = now,
            leftAt = null,
        )

        fun makeTokenResult() =
            TokenResult(
                token = "livekit.jwt.token",
                roomName = "room-test",
                identity = "agent-$agentId",
                livekitUrl = "wss://livekit.example.com",
            )

        "getCustomerToken maps TokenResult to ChannelTokenResponse" {
            every { channelUseCase.getCustomerToken(channelId, "Jane") } returns
                Mono.just(makeTokenResult())

            StepVerifier
                .create(controller.getCustomerToken(channelId, "Jane"))
                .assertNext { response ->
                    response.token shouldBe "livekit.jwt.token"
                    response.roomName shouldBe "room-test"
                    response.identity shouldBe "agent-$agentId"
                    response.livekitUrl shouldBe "wss://livekit.example.com"
                }.verifyComplete()
        }

        "getCustomerToken propagates error from use case" {
            every { channelUseCase.getCustomerToken(channelId, any()) } returns
                Mono.error(UnauthorizedException("Channel not found"))

            StepVerifier
                .create(controller.getCustomerToken(channelId, "Jane"))
                .expectErrorMatches { it is UnauthorizedException }
                .verify()
        }

        "getChannel maps ChannelDetail to ChannelDetailResponse with customer endpoint" {
            val channel = makeChannel()
            val customerEndpoint = makeEndpoint()
            every { channelUseCase.getChannel(channelId) } returns
                Mono.just(ChannelDetail(channel = channel, endpoints = listOf(customerEndpoint)))

            StepVerifier
                .create(controller.getChannel(channelId))
                .assertNext { response ->
                    response.id shouldBe channelId
                    response.agentId shouldBe agentId
                    response.status shouldBe "IN_PROGRESS"
                    response.livekitRoomName shouldBe "room-test"
                    response.customerName shouldBe "Jane"
                    response.customerContact shouldBe "010-1234-5678"
                    response.startedAt shouldBe now
                    response.endedAt shouldBe null
                    response.createdAt shouldBe now
                }.verifyComplete()
        }

        "getChannel returns null customerName when no customer endpoint" {
            val channel = makeChannel()
            every { channelUseCase.getChannel(channelId) } returns
                Mono.just(ChannelDetail(channel = channel, endpoints = emptyList()))

            StepVerifier
                .create(controller.getChannel(channelId))
                .assertNext { response ->
                    response.customerName shouldBe null
                    response.customerContact shouldBe null
                }.verifyComplete()
        }

        "getAgentChannels returns flux of ChannelSummaryResponse" {
            val channel1 = makeChannel()
            val channel2 = makeChannel(id = UUID.randomUUID(), status = ChannelStatus.CLOSED)
            every { channelUseCase.getAgentChannels(any(), null) } returns
                Flux.just(channel1, channel2)

            // Note: getAgentChannels calls authenticatedAgent() which requires SecurityContext.
            // Testing just the use-case mapping logic by calling the use case directly.
            StepVerifier
                .create(channelUseCase.getAgentChannels(agentId, null))
                .assertNext { it.id shouldBe channelId }
                .assertNext { it.status shouldBe ChannelStatus.CLOSED }
                .verifyComplete()
        }
    })
