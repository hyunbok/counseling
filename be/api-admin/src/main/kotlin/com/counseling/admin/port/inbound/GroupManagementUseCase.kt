package com.counseling.admin.port.inbound

import com.counseling.admin.domain.Group
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

interface GroupManagementUseCase {
    fun listGroups(): Flux<Group>

    fun createGroup(name: String): Mono<Group>

    fun updateGroup(
        id: UUID,
        name: String?,
        status: String?,
    ): Mono<Group>

    fun deleteGroup(id: UUID): Mono<Void>
}
