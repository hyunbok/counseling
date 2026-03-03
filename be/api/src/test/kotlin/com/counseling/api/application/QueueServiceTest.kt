package com.counseling.api.application

import com.counseling.api.config.LiveKitProperties
import com.counseling.api.domain.Agent
import com.counseling.api.domain.AgentRole
import com.counseling.api.domain.AgentStatus
import com.counseling.api.domain.Channel
import com.counseling.api.domain.ChannelStatus
import com.counseling.api.domain.Endpoint
import com.counseling.api.domain.EndpointType
import com.counseling.api.domain.PositionUpdate
import com.counseling.api.domain.QueueEntry
import com.counseling.api.domain.QueueUpdate
import com.counseling.api.domain.QueueUpdateType
import com.counseling.api.domain.TenantContext
import com.counseling.api.domain.exception.ConflictException
import com.counseling.api.port.inbound.NotificationUseCase
import com.counseling.api.port.outbound.AgentRepository
import com.counseling.api.port.outbound.ChannelRepository
import com.counseling.api.port.outbound.EndpointRepository
import com.counseling.api.port.outbound.GroupRepository
import com.counseling.api.port.outbound.HistoryReadRepository
import com.counseling.api.port.outbound.LiveKitPort
import com.counseling.api.port.outbound.QueueNotificationPort
import com.counseling.api.port.outbound.QueueRepository
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import reactor.util.context.Context
import java.time.Instant
import java.util.UUID

class QueueServiceTest :
    StringSpec({
        val queueRepository = mockk<QueueRepository>()
        val queueNotificationPort = mockk<QueueNotificationPort>(relaxed = true)
        val channelRepository = mockk<ChannelRepository>()
        val endpointRepository = mockk<EndpointRepository>()
        val agentRepository = mockk<AgentRepository>()
        val liveKitPort = mockk<LiveKitPort>()
        val notificationUseCase = mockk<NotificationUseCase>(relaxed = true)
        val historyReadRepository = mockk<HistoryReadRepository>(relaxed = true)
        val groupRepository = mockk<GroupRepository>(relaxed = true)
        val liveKitProperties =
            LiveKitProperties(
                url = "wss://livekit.test",
                apiKey = "test-key",
                apiSecret = "test-secret",
                tokenTtlSeconds = 3600L,
            )
        val queueService =
            QueueService(
                queueRepository,
                queueNotificationPort,
                channelRepository,
                endpointRepository,
                agentRepository,
                liveKitPort,
                liveKitProperties,
                notificationUseCase,
                historyReadRepository,
                groupRepository,
            )

        val tenantId = "tenant-test"
        val tenantContext: Context = TenantContext.withTenantId(Context.empty(), tenantId)

        afterEach { clearAllMocks() }

        fun makeEntry(groupId: UUID? = null): QueueEntry =
            QueueEntry(
                id = UUID.randomUUID(),
                customerName = "Test Customer",
                customerContact = "010-0000-0000",
                groupId = groupId,
                enteredAt = Instant.now(),
            )

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

        "enterQueue creates entry and returns position and size" {
            val entry = makeEntry()

            every { queueRepository.add(tenantId, any()) } returns Mono.just(true)
            every { queueRepository.getPosition(tenantId, any()) } returns Mono.just(1L)
            every { queueRepository.getSize(tenantId) } returns Mono.just(1L)

            StepVerifier
                .create(
                    queueService
                        .enterQueue("Test Customer", "010-0000-0000", null)
                        .contextWrite(tenantContext),
                ).assertNext { result ->
                    result.position shouldBe 1L
                    result.queueSize shouldBe 1L
                    result.entry.customerName shouldBe "Test Customer"
                }.verifyComplete()

            verify { queueNotificationPort.emitQueueUpdate(tenantId, any()) }
        }

        "enterQueue emits ENTERED queue update" {
            val updateSlot = slot<QueueUpdate>()

            every { queueRepository.add(tenantId, any()) } returns Mono.just(true)
            every { queueRepository.getPosition(tenantId, any()) } returns Mono.just(2L)
            every { queueRepository.getSize(tenantId) } returns Mono.just(2L)
            every { queueNotificationPort.emitQueueUpdate(tenantId, capture(updateSlot)) } returns Unit

            StepVerifier
                .create(
                    queueService
                        .enterQueue("Alice", "010-1111-2222", null)
                        .contextWrite(tenantContext),
                ).assertNext { }
                .verifyComplete()

            updateSlot.captured.type shouldBe QueueUpdateType.ENTERED
            updateSlot.captured.queueSize shouldBe 2L
        }

        "leaveQueue removes entry and emits LEFT update" {
            val entry = makeEntry()
            val updateSlot = slot<QueueUpdate>()

            every { queueRepository.remove(tenantId, entry.id) } returns Mono.just(entry)
            every { queueRepository.getSize(tenantId) } returns Mono.just(0L)
            every { queueNotificationPort.emitQueueUpdate(tenantId, capture(updateSlot)) } returns Unit

            StepVerifier
                .create(
                    queueService
                        .leaveQueue(entry.id)
                        .contextWrite(tenantContext),
                ).verifyComplete()

            updateSlot.captured.type shouldBe QueueUpdateType.LEFT
            verify { queueNotificationPort.emitPositionUpdate(tenantId, any()) }
        }

        "leaveQueue emits position update with position=0" {
            val entry = makeEntry()
            val positionSlot = slot<PositionUpdate>()

            every { queueRepository.remove(tenantId, entry.id) } returns Mono.just(entry)
            every { queueRepository.getSize(tenantId) } returns Mono.just(0L)
            every { queueNotificationPort.emitPositionUpdate(tenantId, capture(positionSlot)) } returns Unit

            StepVerifier
                .create(
                    queueService
                        .leaveQueue(entry.id)
                        .contextWrite(tenantContext),
                ).verifyComplete()

            positionSlot.captured.position shouldBe 0L
            positionSlot.captured.entryId shouldBe entry.id
        }

        "acceptCustomer throws ConflictException when agent is not available" {
            val agent = makeAgent(AgentStatus.BUSY)
            val entryId = UUID.randomUUID()

            every { agentRepository.findByIdAndNotDeleted(agent.id) } returns Mono.just(agent)

            StepVerifier
                .create(
                    queueService
                        .acceptCustomer(entryId, agent.id)
                        .contextWrite(tenantContext),
                ).expectErrorMatches { it is ConflictException }
                .verify()
        }

        "acceptCustomer creates channel and endpoints when agent is available" {
            val agent = makeAgent(AgentStatus.ONLINE)
            val entry = makeEntry()
            val savedChannels = mutableListOf<Channel>()
            val savedEndpoints = mutableListOf<Endpoint>()
            val testRoomName = "$tenantId-channel-test"

            every { agentRepository.findByIdAndNotDeleted(agent.id) } returns Mono.just(agent)
            every { queueRepository.removeAtomically(tenantId, entry.id) } returns Mono.just(entry)
            every { channelRepository.save(any()) } answers {
                val ch = firstArg<Channel>()
                savedChannels.add(ch)
                Mono.just(ch)
            }
            every { endpointRepository.save(any()) } answers {
                val ep = firstArg<Endpoint>()
                savedEndpoints.add(ep)
                Mono.just(ep)
            }
            every { agentRepository.save(any()) } returns Mono.just(agent.updateStatus(AgentStatus.BUSY))
            every { liveKitPort.createRoom(any()) } returns Mono.just(testRoomName)
            every { liveKitPort.generateToken(any(), any(), any(), any(), any()) } returns "test-jwt-token"
            every { queueRepository.getSize(tenantId) } returns Mono.just(0L)
            every { historyReadRepository.upsert(any()) } returns Mono.empty()

            StepVerifier
                .create(
                    queueService
                        .acceptCustomer(entry.id, agent.id)
                        .contextWrite(tenantContext),
                ).assertNext { result ->
                    result.customerName shouldBe entry.customerName
                    result.customerContact shouldBe entry.customerContact
                    result.livekitRoomName shouldBe testRoomName
                    result.livekitUrl shouldBe liveKitProperties.url
                }.verifyComplete()

            savedChannels.size shouldBe 1
            savedChannels[0].status shouldBe ChannelStatus.IN_PROGRESS
            savedChannels[0].agentId shouldBe agent.id
            savedChannels[0].livekitRoomName shouldBe testRoomName
            savedEndpoints.any { it.type == EndpointType.CUSTOMER } shouldBe true
            savedEndpoints.any { it.type == EndpointType.AGENT } shouldBe true
            verify { agentRepository.save(match { it.agentStatus == AgentStatus.BUSY }) }
            verify { queueNotificationPort.emitQueueUpdate(tenantId, match { it.type == QueueUpdateType.ACCEPTED }) }
            verify { queueNotificationPort.emitPositionUpdate(tenantId, match { it.position == 0L }) }
        }

        "getQueue returns entries with position and wait duration" {
            val entry1 = makeEntry()
            val entry2 = makeEntry()

            every { queueRepository.findAll(tenantId) } returns Flux.just(entry1, entry2)

            StepVerifier
                .create(
                    queueService
                        .getQueue()
                        .contextWrite(tenantContext),
                ).assertNext { item ->
                    item.position shouldBe 1L
                    item.entry shouldBe entry1
                }.assertNext { item ->
                    item.position shouldBe 2L
                    item.entry shouldBe entry2
                }.verifyComplete()
        }

        "getPosition returns position and size" {
            val entryId = UUID.randomUUID()

            every { queueRepository.getPosition(tenantId, entryId) } returns Mono.just(3L)
            every { queueRepository.getSize(tenantId) } returns Mono.just(5L)

            StepVerifier
                .create(
                    queueService
                        .getPosition(entryId)
                        .contextWrite(tenantContext),
                ).assertNext { result ->
                    result.position shouldBe 3L
                    result.queueSize shouldBe 5L
                }.verifyComplete()
        }
    })
