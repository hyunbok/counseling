package com.counseling.api.application

import com.counseling.api.config.LiveKitProperties
import com.counseling.api.domain.Agent
import com.counseling.api.domain.AgentRole
import com.counseling.api.domain.AgentStatus
import com.counseling.api.domain.Channel
import com.counseling.api.domain.ChannelStatus
import com.counseling.api.domain.Endpoint
import com.counseling.api.domain.EndpointType
import com.counseling.api.domain.TenantContext
import com.counseling.api.domain.exception.ConflictException
import com.counseling.api.domain.exception.NotFoundException
import com.counseling.api.port.outbound.AgentRepository
import com.counseling.api.port.outbound.ChannelRepository
import com.counseling.api.port.outbound.EndpointRepository
import com.counseling.api.port.outbound.LiveKitPort
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import reactor.util.context.Context
import java.time.Instant
import java.util.UUID

class ChannelServiceTest :
    StringSpec({
        val channelRepository = mockk<ChannelRepository>()
        val endpointRepository = mockk<EndpointRepository>()
        val agentRepository = mockk<AgentRepository>()
        val liveKitPort = mockk<LiveKitPort>()
        val liveKitProperties =
            LiveKitProperties(
                url = "wss://livekit.example.com",
                apiKey = "key",
                apiSecret = "secret",
                tokenTtlSeconds = 3600L,
            )
        val channelService =
            ChannelService(
                channelRepository,
                endpointRepository,
                agentRepository,
                liveKitPort,
                liveKitProperties,
            )

        val tenantId = "tenant-test"
        val tenantContext: Context = TenantContext.withTenantId(Context.empty(), tenantId)

        afterEach { clearAllMocks() }

        fun makeAgent(status: AgentStatus = AgentStatus.ONLINE): Agent =
            Agent(
                id = UUID.randomUUID(),
                username = "agent1",
                passwordHash = "hash",
                name = "Agent One",
                role = AgentRole.COUNSELOR,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
                agentStatus = status,
            )

        fun makeChannel(
            agentId: UUID,
            status: ChannelStatus = ChannelStatus.IN_PROGRESS,
            roomName: String? = "tenant-channel-id",
        ): Channel {
            val now = Instant.now()
            return Channel(
                id = UUID.randomUUID(),
                agentId = agentId,
                status = status,
                startedAt = now,
                endedAt = null,
                recordingPath = null,
                livekitRoomName = roomName,
                createdAt = now,
                updatedAt = now,
            )
        }

        fun makeEndpoint(
            channelId: UUID,
            type: EndpointType,
            customerName: String? = null,
        ): Endpoint =
            Endpoint(
                id = UUID.randomUUID(),
                channelId = channelId,
                type = type,
                customerName = customerName,
                customerContact = null,
                joinedAt = Instant.now(),
                leftAt = null,
            )

        "getAgentToken returns token for valid agent and open channel" {
            val agent = makeAgent()
            val channel = makeChannel(agentId = agent.id, roomName = "room-123")

            every { channelRepository.findByIdAndNotDeleted(channel.id) } returns Mono.just(channel)
            every { agentRepository.findByIdAndNotDeleted(agent.id) } returns Mono.just(agent)
            every {
                liveKitPort.generateToken("room-123", "agent:${agent.id}", agent.name, true, true)
            } returns "agent-jwt-token"

            StepVerifier
                .create(channelService.getAgentToken(channel.id, agent.id))
                .assertNext { result ->
                    result.token shouldBe "agent-jwt-token"
                    result.roomName shouldBe "room-123"
                    result.identity shouldBe "agent:${agent.id}"
                    result.livekitUrl shouldBe "wss://livekit.example.com"
                }.verifyComplete()
        }

        "getAgentToken throws ConflictException when agent is not channel owner" {
            val agent = makeAgent()
            val otherAgentId = UUID.randomUUID()
            val channel = makeChannel(agentId = otherAgentId, roomName = "room-123")

            every { channelRepository.findByIdAndNotDeleted(channel.id) } returns Mono.just(channel)

            StepVerifier
                .create(channelService.getAgentToken(channel.id, agent.id))
                .expectErrorMatches { it is ConflictException }
                .verify()
        }

        "getAgentToken throws NotFoundException when channel not found" {
            val channelId = UUID.randomUUID()
            val agentId = UUID.randomUUID()

            every { channelRepository.findByIdAndNotDeleted(channelId) } returns Mono.empty()

            StepVerifier
                .create(channelService.getAgentToken(channelId, agentId))
                .expectErrorMatches { it is NotFoundException }
                .verify()
        }

        "getAgentToken throws ConflictException when channel is closed" {
            val agent = makeAgent()
            val channel = makeChannel(agentId = agent.id, status = ChannelStatus.CLOSED, roomName = "room-123")

            every { channelRepository.findByIdAndNotDeleted(channel.id) } returns Mono.just(channel)

            StepVerifier
                .create(channelService.getAgentToken(channel.id, agent.id))
                .expectErrorMatches { it is ConflictException }
                .verify()
        }

        "getCustomerToken returns token for valid customer with existing endpoint" {
            val agentId = UUID.randomUUID()
            val channel = makeChannel(agentId = agentId, roomName = "room-456")
            val endpoint = makeEndpoint(channel.id, EndpointType.CUSTOMER, customerName = "Alice")

            every { channelRepository.findByIdAndNotDeleted(channel.id) } returns Mono.just(channel)
            every { endpointRepository.findAllByChannelId(channel.id) } returns Flux.just(endpoint)
            every {
                liveKitPort.generateToken("room-456", "customer:Alice", "Alice", true, true)
            } returns "customer-jwt-token"

            StepVerifier
                .create(channelService.getCustomerToken(channel.id, "Alice"))
                .assertNext { result ->
                    result.token shouldBe "customer-jwt-token"
                    result.roomName shouldBe "room-456"
                    result.identity shouldBe "customer:Alice"
                    result.livekitUrl shouldBe "wss://livekit.example.com"
                }.verifyComplete()
        }

        "getCustomerToken throws NotFoundException when customer endpoint not found" {
            val agentId = UUID.randomUUID()
            val channel = makeChannel(agentId = agentId, roomName = "room-456")
            val agentEndpoint = makeEndpoint(channel.id, EndpointType.AGENT)

            every { channelRepository.findByIdAndNotDeleted(channel.id) } returns Mono.just(channel)
            every { endpointRepository.findAllByChannelId(channel.id) } returns Flux.just(agentEndpoint)

            StepVerifier
                .create(channelService.getCustomerToken(channel.id, "Bob"))
                .expectErrorMatches { it is NotFoundException }
                .verify()
        }

        "closeChannel closes channel, updates endpoints, and sets agent ONLINE" {
            val agent = makeAgent(AgentStatus.BUSY)
            val channel = makeChannel(agentId = agent.id, roomName = "room-789")
            val customerEndpoint = makeEndpoint(channel.id, EndpointType.CUSTOMER, "Alice")
            val agentEndpoint = makeEndpoint(channel.id, EndpointType.AGENT)

            every { channelRepository.findByIdAndNotDeleted(channel.id) } returns Mono.just(channel)
            every { liveKitPort.deleteRoom("room-789") } returns Mono.empty()
            every {
                endpointRepository.findAllByChannelId(channel.id)
            } returns Flux.just(customerEndpoint, agentEndpoint)
            every { endpointRepository.save(any()) } answers { Mono.just(firstArg()) }
            every { channelRepository.save(any()) } answers { Mono.just(firstArg()) }
            every { agentRepository.findByIdAndNotDeleted(agent.id) } returns Mono.just(agent)
            every { agentRepository.save(any()) } answers { Mono.just(firstArg()) }

            StepVerifier
                .create(channelService.closeChannel(channel.id, agent.id))
                .verifyComplete()

            verify { liveKitPort.deleteRoom("room-789") }
            verify { channelRepository.save(match { it.status == ChannelStatus.CLOSED }) }
            verify { agentRepository.save(match { it.agentStatus == AgentStatus.ONLINE }) }
        }

        "closeChannel throws ConflictException when agent is not channel owner" {
            val agentId = UUID.randomUUID()
            val otherAgentId = UUID.randomUUID()
            val channel = makeChannel(agentId = otherAgentId, roomName = "room-789")

            every { channelRepository.findByIdAndNotDeleted(channel.id) } returns Mono.just(channel)

            StepVerifier
                .create(channelService.closeChannel(channel.id, agentId))
                .expectErrorMatches { it is ConflictException }
                .verify()
        }

        "closeChannel throws ConflictException when channel is already closed" {
            val agent = makeAgent()
            val channel = makeChannel(agentId = agent.id, status = ChannelStatus.CLOSED, roomName = "room-789")

            every { channelRepository.findByIdAndNotDeleted(channel.id) } returns Mono.just(channel)

            StepVerifier
                .create(channelService.closeChannel(channel.id, agent.id))
                .expectErrorMatches { it is ConflictException }
                .verify()
        }

        "getChannel returns channel with endpoints" {
            val agentId = UUID.randomUUID()
            val channel = makeChannel(agentId = agentId)
            val endpoints =
                listOf(
                    makeEndpoint(channel.id, EndpointType.CUSTOMER, "Alice"),
                    makeEndpoint(channel.id, EndpointType.AGENT),
                )

            every { channelRepository.findByIdAndNotDeleted(channel.id) } returns Mono.just(channel)
            every { endpointRepository.findAllByChannelId(channel.id) } returns Flux.fromIterable(endpoints)

            StepVerifier
                .create(channelService.getChannel(channel.id))
                .assertNext { detail ->
                    detail.channel shouldBe channel
                    detail.endpoints.size shouldBe 2
                }.verifyComplete()
        }

        "getAgentChannels without status returns all agent channels" {
            val agentId = UUID.randomUUID()
            val channel1 = makeChannel(agentId = agentId, status = ChannelStatus.IN_PROGRESS)
            val channel2 = makeChannel(agentId = agentId, status = ChannelStatus.CLOSED)

            every { channelRepository.findAllByAgentIdAndNotDeleted(agentId) } returns Flux.just(channel1, channel2)

            StepVerifier
                .create(channelService.getAgentChannels(agentId, null))
                .assertNext { it.agentId shouldBe agentId }
                .assertNext { it.agentId shouldBe agentId }
                .verifyComplete()
        }

        "getAgentChannels with status filters channels by status" {
            val agentId = UUID.randomUUID()
            val channel = makeChannel(agentId = agentId, status = ChannelStatus.IN_PROGRESS)

            every {
                channelRepository.findAllByAgentIdAndStatusAndNotDeleted(agentId, ChannelStatus.IN_PROGRESS)
            } returns Flux.just(channel)

            StepVerifier
                .create(channelService.getAgentChannels(agentId, ChannelStatus.IN_PROGRESS))
                .assertNext { it.status shouldBe ChannelStatus.IN_PROGRESS }
                .verifyComplete()
        }
    })
