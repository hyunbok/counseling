package com.counseling.api.application

import com.counseling.api.domain.ChatMessage
import com.counseling.api.domain.exception.BadRequestException
import com.counseling.api.domain.exception.NotFoundException
import com.counseling.api.port.inbound.ChatUseCase
import com.counseling.api.port.inbound.SendMessageCommand
import com.counseling.api.port.outbound.ChannelRepository
import com.counseling.api.port.outbound.ChatMessageReadRepository
import com.counseling.api.port.outbound.ChatMessageRepository
import com.counseling.api.port.outbound.ChatNotificationPort
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Service
@Profile("!test")
class ChatService(
    private val channelRepository: ChannelRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val chatMessageReadRepository: ChatMessageReadRepository,
    private val chatNotificationPort: ChatNotificationPort,
) : ChatUseCase {
    override fun sendMessage(command: SendMessageCommand): Mono<ChatMessage> =
        channelRepository
            .findByIdAndNotDeleted(command.channelId)
            .switchIfEmpty(Mono.error(NotFoundException("Channel not found: ${command.channelId}")))
            .flatMap { channel ->
                if (!channel.isOpen()) {
                    return@flatMap Mono.error(BadRequestException("Channel is not open"))
                }
                val message =
                    ChatMessage(
                        id = UUID.randomUUID(),
                        channelId = command.channelId,
                        senderType = command.senderType,
                        senderId = command.senderId,
                        content = command.content,
                        createdAt = Instant.now(),
                    )
                chatMessageRepository
                    .save(message)
                    .flatMap { saved ->
                        chatMessageReadRepository.save(saved).thenReturn(saved)
                    }.doOnNext { saved ->
                        chatNotificationPort.emitMessage(command.channelId, saved)
                    }
            }
}
