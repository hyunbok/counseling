package com.counseling.admin.port.outbound

import com.counseling.admin.domain.Group
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface AdminGroupRepository {
    fun save(group: Group): Mono<Group>

    fun findByIdAndNotDeleted(id: UUID): Mono<Group>

    fun findAllByNotDeleted(): Flux<Group>

    fun findAllByNotDeleted(
        page: Int,
        size: Int,
    ): Flux<Group>

    fun countAllByNotDeleted(): Mono<Long>

    fun findByNameAndNotDeleted(name: String): Mono<Group>

    fun searchByNotDeleted(
        search: String?,
        status: String?,
        page: Int,
        size: Int,
    ): Flux<Group>

    fun countSearchByNotDeleted(
        search: String?,
        status: String?,
    ): Mono<Long>
}
