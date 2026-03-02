package com.counseling.api.adapter.inbound.web.controller

import com.counseling.api.adapter.inbound.web.dto.ChatMessageListResponse
import com.counseling.api.adapter.inbound.web.dto.ChatMessageResponse
import com.counseling.api.adapter.inbound.web.dto.SendChatMessageRequest
import com.counseling.api.domain.ChatMessage
import com.counseling.api.port.inbound.ChatQuery
import com.counseling.api.port.inbound.ChatUseCase
import com.counseling.api.port.inbound.SendMessageCommand
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/channels/{channelId}/chat")
@Profile("!test")
class ChatController(
    private val chatUseCase: ChatUseCase,
    private val chatQuery: ChatQuery,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun sendMessage(
        @PathVariable channelId: UUID,
        @RequestBody request: SendChatMessageRequest,
    ): Mono<ChatMessageResponse> =
        chatUseCase
            .sendMessage(
                SendMessageCommand(
                    channelId = channelId,
                    senderType = request.senderType,
                    senderId = request.senderId,
                    content = request.content,
                ),
            ).map { it.toResponse() }

    @GetMapping
    fun getMessageHistory(
        @PathVariable channelId: UUID,
        @RequestParam(required = false) before: Instant?,
        @RequestParam(defaultValue = "50") limit: Int,
    ): Mono<ChatMessageListResponse> =
        chatQuery
            .getMessageHistory(channelId, before, limit.coerceIn(1, 100))
            .map { result ->
                ChatMessageListResponse(
                    messages = result.messages.map { it.toResponse() },
                    hasMore = result.hasMore,
                    oldestTimestamp = result.oldestTimestamp,
                )
            }

    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamMessages(
        @PathVariable channelId: UUID,
    ): Flux<ChatMessageResponse> = chatQuery.streamMessages(channelId).map { it.toResponse() }

    private fun ChatMessage.toResponse(): ChatMessageResponse =
        ChatMessageResponse(
            id = id,
            channelId = channelId,
            senderType = senderType.name,
            senderId = senderId,
            content = content,
            createdAt = createdAt,
        )
}
