package com.counseling.api.application

import com.counseling.api.domain.ChatMessage
import com.counseling.api.domain.SenderType
import com.counseling.api.port.outbound.ChatMessageReadRepository
import com.counseling.api.port.outbound.ChatNotificationPort
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import reactor.core.publisher.Flux
import reactor.test.StepVerifier
import java.time.Instant
import java.util.UUID

class ChatQueryServiceTest :
    StringSpec({
        val chatMessageReadRepository = mockk<ChatMessageReadRepository>()
        val chatNotificationPort = mockk<ChatNotificationPort>()
        val chatQueryService = ChatQueryService(chatMessageReadRepository, chatNotificationPort)

        afterEach { clearAllMocks() }

        fun makeMessage(
            channelId: UUID,
            createdAt: Instant = Instant.now(),
        ): ChatMessage =
            ChatMessage(
                id = UUID.randomUUID(),
                channelId = channelId,
                senderType = SenderType.AGENT,
                senderId = "agent-1",
                content = "message",
                createdAt = createdAt,
            )

        "getMessageHistory should return messages with hasMore=false when fewer than limit" {
            val channelId = UUID.randomUUID()
            val baseTime = Instant.now()
            val messages =
                (1..3).map { i ->
                    makeMessage(channelId, baseTime.plusSeconds(i.toLong()))
                }
            val limit = 50

            every {
                chatMessageReadRepository.findByChannelId(channelId, null, limit + 1)
            } returns Flux.fromIterable(messages)

            StepVerifier
                .create(chatQueryService.getMessageHistory(channelId, null, limit))
                .assertNext { result ->
                    result.hasMore shouldBe false
                    result.messages.size shouldBe 3
                }.verifyComplete()
        }

        "getMessageHistory should return hasMore=true when more messages exist" {
            val channelId = UUID.randomUUID()
            val limit = 3
            val baseTime = Instant.now()
            // Return limit+1 messages to signal there are more
            val messages =
                (1..(limit + 1)).map { i ->
                    makeMessage(channelId, baseTime.plusSeconds(i.toLong()))
                }

            every {
                chatMessageReadRepository.findByChannelId(channelId, null, limit + 1)
            } returns Flux.fromIterable(messages)

            StepVerifier
                .create(chatQueryService.getMessageHistory(channelId, null, limit))
                .assertNext { result ->
                    result.hasMore shouldBe true
                    result.messages.size shouldBe limit
                }.verifyComplete()
        }

        "streamMessages should delegate to notification port" {
            val channelId = UUID.randomUUID()
            val message = makeMessage(channelId)

            every { chatNotificationPort.subscribeMessages(channelId) } returns Flux.just(message)

            StepVerifier
                .create(chatQueryService.streamMessages(channelId))
                .assertNext { it shouldBe message }
                .verifyComplete()
        }
    })
