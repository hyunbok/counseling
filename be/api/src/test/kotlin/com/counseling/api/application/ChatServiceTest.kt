package com.counseling.api.application

import com.counseling.api.domain.Channel
import com.counseling.api.domain.ChannelStatus
import com.counseling.api.domain.ChatMessage
import com.counseling.api.domain.SenderType
import com.counseling.api.domain.exception.BadRequestException
import com.counseling.api.domain.exception.NotFoundException
import com.counseling.api.port.inbound.SendMessageCommand
import com.counseling.api.port.outbound.ChannelRepository
import com.counseling.api.port.outbound.ChatMessageReadRepository
import com.counseling.api.port.outbound.ChatMessageRepository
import com.counseling.api.port.outbound.ChatNotificationPort
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant
import java.util.UUID

class ChatServiceTest :
    StringSpec({
        val channelRepository = mockk<ChannelRepository>()
        val chatMessageRepository = mockk<ChatMessageRepository>()
        val chatMessageReadRepository = mockk<ChatMessageReadRepository>()
        val chatNotificationPort = mockk<ChatNotificationPort>(relaxed = true)
        val chatService =
            ChatService(
                channelRepository,
                chatMessageRepository,
                chatMessageReadRepository,
                chatNotificationPort,
            )

        afterEach { clearAllMocks() }

        fun makeChannel(status: ChannelStatus = ChannelStatus.IN_PROGRESS): Channel {
            val now = Instant.now()
            return Channel(
                id = UUID.randomUUID(),
                agentId = UUID.randomUUID(),
                status = status,
                startedAt = now,
                endedAt = null,
                recordingPath = null,
                livekitRoomName = "room-1",
                createdAt = now,
                updatedAt = now,
            )
        }

        fun makeMessage(channelId: UUID): ChatMessage =
            ChatMessage(
                id = UUID.randomUUID(),
                channelId = channelId,
                senderType = SenderType.AGENT,
                senderId = "agent-1",
                content = "Hello",
                createdAt = Instant.now(),
            )

        "sendMessage should save message and emit notification when channel is open" {
            val channel = makeChannel(ChannelStatus.IN_PROGRESS)
            val command =
                SendMessageCommand(
                    channelId = channel.id,
                    senderType = SenderType.AGENT,
                    senderId = "agent-1",
                    content = "Hello",
                )

            every { channelRepository.findByIdAndNotDeleted(channel.id) } returns Mono.just(channel)
            every { chatMessageRepository.save(any()) } answers {
                Mono.just(firstArg())
            }
            every { chatMessageReadRepository.save(any()) } answers {
                Mono.just(firstArg())
            }

            StepVerifier
                .create(chatService.sendMessage(command))
                .assertNext { saved ->
                    saved.channelId shouldBe channel.id
                    saved.content shouldBe "Hello"
                }.verifyComplete()

            verify { chatMessageRepository.save(any()) }
            verify { chatMessageReadRepository.save(any()) }
            verify { chatNotificationPort.emitMessage(channel.id, any()) }
        }

        "sendMessage should fail with NotFoundException when channel not found" {
            val channelId = UUID.randomUUID()
            val command =
                SendMessageCommand(
                    channelId = channelId,
                    senderType = SenderType.CUSTOMER,
                    senderId = "customer-1",
                    content = "Hi",
                )

            every { channelRepository.findByIdAndNotDeleted(channelId) } returns Mono.empty()

            StepVerifier
                .create(chatService.sendMessage(command))
                .expectError(NotFoundException::class.java)
                .verify()
        }

        "sendMessage should fail with BadRequestException when channel is closed" {
            val channel = makeChannel(ChannelStatus.CLOSED)
            val command =
                SendMessageCommand(
                    channelId = channel.id,
                    senderType = SenderType.AGENT,
                    senderId = "agent-1",
                    content = "Too late",
                )

            every { channelRepository.findByIdAndNotDeleted(channel.id) } returns Mono.just(channel)

            StepVerifier
                .create(chatService.sendMessage(command))
                .expectError(BadRequestException::class.java)
                .verify()
        }
    })
