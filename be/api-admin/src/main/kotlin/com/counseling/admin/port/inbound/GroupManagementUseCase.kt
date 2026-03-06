package com.counseling.admin.port.inbound

import com.counseling.admin.domain.Group
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

data class GroupWithAgentCount(
    val group: Group,
    val agentCount: Int,
)

interface GroupManagementUseCase {
    fun listGroups(): Flux<Group>

    fun listGroupsWithAgentCount(): Flux<GroupWithAgentCount>

    fun listGroupsPaged(
        search: String?,
        status: String?,
        page: Int,
        size: Int,
    ): Mono<PagedResult<GroupWithAgentCount>>

    fun createGroup(name: String): Mono<Group>

    fun updateGroup(
        id: UUID,
        name: String?,
        status: String?,
    ): Mono<Group>

    fun deleteGroup(id: UUID): Mono<Void>
}
