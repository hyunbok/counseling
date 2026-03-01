package com.counseling.api.domain.event

import com.counseling.api.domain.Channel
import java.time.Instant

sealed class ChannelEvent : DomainEvent {
    data class Created(
        val channel: Channel,
        override val occurredAt: Instant = Instant.now(),
    ) : ChannelEvent()

    data class Started(
        val channel: Channel,
        override val occurredAt: Instant = Instant.now(),
    ) : ChannelEvent()

    data class Closed(
        val channel: Channel,
        override val occurredAt: Instant = Instant.now(),
    ) : ChannelEvent()

    data class RoomCreated(
        val channel: Channel,
        val livekitRoomName: String,
        override val occurredAt: Instant = Instant.now(),
    ) : ChannelEvent()
}
