package com.counseling.api.adapter.outbound.external

import com.counseling.api.domain.exception.LiveKitException
import com.counseling.api.port.outbound.EgressStartResult
import com.counseling.api.port.outbound.LiveKitEgressPort
import io.livekit.server.EgressServiceClient
import livekit.LivekitEgress
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import retrofit2.Call

@Component
@Profile("!test")
class LiveKitEgressAdapter(
    private val egressServiceClient: EgressServiceClient,
) : LiveKitEgressPort {
    override fun startRoomCompositeEgress(
        roomName: String,
        filePath: String,
    ): Mono<EgressStartResult> {
        val fileOutput =
            LivekitEgress.EncodedFileOutput
                .newBuilder()
                .setFilepath(filePath)
                .setFileType(LivekitEgress.EncodedFileType.MP4)
                .build()
        return egressServiceClient
            .startRoomCompositeEgress(roomName, fileOutput)
            .toMono()
            .map { egressInfo -> EgressStartResult(egressId = egressInfo.egressId) }
    }

    override fun stopEgress(egressId: String): Mono<Void> =
        Mono
            .fromCallable { egressServiceClient.stopEgress(egressId).execute() }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap<Void> { response ->
                if (response.isSuccessful) {
                    Mono.empty()
                } else {
                    Mono.error(
                        LiveKitException(
                            "LiveKit Egress API error: ${response.code()} ${response.message()}",
                        ),
                    )
                }
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
                            "LiveKit Egress API error: ${response.code()} ${response.message()}",
                        ),
                    )
                }
            }
}
