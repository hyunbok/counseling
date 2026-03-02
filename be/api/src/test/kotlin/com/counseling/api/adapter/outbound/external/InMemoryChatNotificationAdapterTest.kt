package com.counseling.api.adapter.outbound.external

import com.counseling.api.domain.ChatMessage
import com.counseling.api.domain.SenderType
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import reactor.test.StepVerifier
import java.time.Instant
import java.util.UUID

class InMemoryChatNotificationAdapterTest :
    StringSpec({
        fun makeMessage(channelId: UUID): ChatMessage =
            ChatMessage(
                id = UUID.randomUUID(),
                channelId = channelId,
                senderType = SenderType.AGENT,
                senderId = "agent-1",
                content = "Hello",
                createdAt = Instant.now(),
            )

        "emitMessage should deliver to subscriber" {
            val adapter = InMemoryChatNotificationAdapter()
            val channelId = UUID.randomUUID()
            val message = makeMessage(channelId)

            val flux = adapter.subscribeMessages(channelId)

            StepVerifier
                .create(flux)
                .then { adapter.emitMessage(channelId, message) }
                .assertNext { received ->
                    received shouldBe message
                }.thenCancel()
                .verify()
        }

        "messages should be isolated per channel" {
            val adapter = InMemoryChatNotificationAdapter()
            val channelA = UUID.randomUUID()
            val channelB = UUID.randomUUID()
            val messageA = makeMessage(channelA)

            val fluxB = adapter.subscribeMessages(channelB)

            StepVerifier
                .create(fluxB)
                .then { adapter.emitMessage(channelA, messageA) }
                .thenCancel()
                .verify()

            // Channel A subscriber should receive the message
            val fluxA = adapter.subscribeMessages(channelA)
            val messageA2 = makeMessage(channelA)

            StepVerifier
                .create(fluxA)
                .then { adapter.emitMessage(channelA, messageA2) }
                .assertNext { received ->
                    received.channelId shouldBe channelA
                }.thenCancel()
                .verify()
        }
    })
