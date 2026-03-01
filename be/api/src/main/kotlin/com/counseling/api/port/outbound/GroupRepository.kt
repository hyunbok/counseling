package com.counseling.api.port.outbound

import com.counseling.api.domain.Group
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupRepository {
    fun save(group: Group): Mono<Group>

    fun findByIdAndNotDeleted(id: UUID): Mono<Group>

    fun findAllByNotDeleted(): Flux<Group>
}
