package com.counseling.api.config

import io.livekit.server.RoomServiceClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class LiveKitConfig {
    @Bean
    @Profile("!test")
    fun roomServiceClient(props: LiveKitProperties): RoomServiceClient =
        RoomServiceClient.create(props.url, props.apiKey, props.apiSecret)
}
