package com.counseling.api.adapter.outbound.external

import com.counseling.api.config.LiveKitProperties
import com.counseling.api.domain.exception.LiveKitException
import com.counseling.api.port.outbound.LiveKitPort
import io.livekit.server.AccessToken
import io.livekit.server.CanPublish
import io.livekit.server.CanPublishData
import io.livekit.server.CanSubscribe
import io.livekit.server.RoomJoin
import io.livekit.server.RoomName
import io.livekit.server.RoomServiceClient
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import retrofit2.Call

@Component
@Profile("!test")
class LiveKitAdapter(
    private val roomServiceClient: RoomServiceClient,
    private val liveKitProperties: LiveKitProperties,
) : LiveKitPort {
    override fun createRoom(
        roomName: String,
        emptyTimeoutSec: Int,
        maxParticipants: Int,
    ): Mono<String> =
        roomServiceClient
            .createRoom(roomName, emptyTimeoutSec, maxParticipants)
            .toMono()
            .map { it.name }

    override fun deleteRoom(roomName: String): Mono<Void> =
        Mono
            .fromCallable { roomServiceClient.deleteRoom(roomName).execute() }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap<Void> { response ->
                if (response.isSuccessful) {
                    Mono.empty()
                } else if (response.code() == 404) {
                    Mono.empty()
                } else {
                    Mono.error(
                        LiveKitException(
                            "LiveKit API error: ${response.code()} ${response.message()}",
                        ),
                    )
                }
            }

    override fun generateToken(
        roomName: String,
        identity: String,
        name: String,
        canPublish: Boolean,
        canSubscribe: Boolean,
    ): String {
        val token =
            AccessToken(liveKitProperties.apiKey, liveKitProperties.apiSecret).apply {
                this.identity = identity
                this.name = name
                this.ttl = liveKitProperties.tokenTtlSeconds
                addGrants(
                    RoomJoin(true),
                    RoomName(roomName),
                    CanPublish(canPublish),
                    CanSubscribe(canSubscribe),
                    CanPublishData(true),
                )
            }
        return token.toJwt()
    }

    private fun <T : Any> Call<T>.toMono(): Mono<T> =
        Mono
            .fromCallable { this.execute() }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap { response ->
                if (response.isSuccessful) {
                    Mono.justOrEmpty(response.body())
                } else {
                    Mono.error<T>(
                        LiveKitException(
                            "LiveKit API error: ${response.code()} ${response.message()}",
                        ),
                    )
                }
            }
}
