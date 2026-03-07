package com.counseling.api.adapter.inbound.web.controller

import com.counseling.api.port.inbound.WebRtcEventHandler
import io.livekit.server.WebhookReceiver
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/webhook/livekit")
@Profile("!test")
class LiveKitWebhookController(
    private val webhookReceiver: WebhookReceiver,
    private val webRtcEventHandler: WebRtcEventHandler,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping
    fun handleWebhook(
        @RequestHeader("Authorization") authHeader: String,
        @RequestBody body: String,
    ): Mono<ResponseEntity<Void>> {
        val event =
            try {
                webhookReceiver.receive(body, authHeader)
            } catch (e: Exception) {
                log.warn("Invalid webhook signature: {}", e.message)
                return Mono.just(ResponseEntity.badRequest().build())
            }

        val eventType = event.event
        val roomName = event.room?.name

        log.info("LiveKit webhook: event={}, room={}", eventType, roomName)

        if (eventType == "room_finished" && !roomName.isNullOrBlank()) {
            return webRtcEventHandler
                .onRoomFinished(roomName)
                .thenReturn(ResponseEntity.ok().build<Void>())
                .onErrorResume { e ->
                    log.error("Error handling room_finished for {}: {}", roomName, e.message, e)
                    Mono.just(ResponseEntity.ok().build())
                }
        }

        return Mono.just(ResponseEntity.ok().build())
    }
}
