package com.counseling.admin.port.outbound

import com.counseling.admin.domain.Group
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface AdminGroupRepository {
    fun save(group: Group): Mono<Group>

    fun findByIdAndNotDeleted(id: String): Mono<Group>

    fun findAllByNotDeleted(): Flux<Group>

    fun findAllByNotDeleted(
        page: Int,
        size: Int,
    ): Flux<Group>

    fun countAllByNotDeleted(): Mono<Long>

    fun findByNameAndNotDeleted(name: String): Mono<Group>
}
