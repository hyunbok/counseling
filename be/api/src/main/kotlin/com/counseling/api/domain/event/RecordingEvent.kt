package com.counseling.api.domain.event

import com.counseling.api.domain.Recording
import java.time.Instant

sealed class RecordingEvent : DomainEvent {
    data class Started(
        val recording: Recording,
        override val occurredAt: Instant = Instant.now(),
    ) : RecordingEvent()

    data class Stopped(
        val recording: Recording,
        override val occurredAt: Instant = Instant.now(),
    ) : RecordingEvent()

    data class Failed(
        val recording: Recording,
        override val occurredAt: Instant = Instant.now(),
    ) : RecordingEvent()
}
