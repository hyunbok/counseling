package com.counseling.api.port.outbound

import com.counseling.api.domain.QueueEntry
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface QueueRepository {
    fun add(
        tenantId: String,
        entry: QueueEntry,
    ): Mono<Boolean>

    /**
     * Removes an entry from the queue. Returns empty Mono if not found.
     */
    fun remove(
        tenantId: String,
        entryId: UUID,
    ): Mono<QueueEntry>

    fun findAll(tenantId: String): Flux<QueueEntry>

    fun findById(
        tenantId: String,
        entryId: UUID,
    ): Mono<QueueEntry>

    fun getPosition(
        tenantId: String,
        entryId: UUID,
    ): Mono<Long>

    fun getSize(tenantId: String): Mono<Long>

    fun removeAtomically(
        tenantId: String,
        entryId: UUID,
    ): Mono<QueueEntry>
}
