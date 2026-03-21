package com.counseling.admin.port.inbound

import com.counseling.admin.domain.Group
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

data class GroupWithAgentCount(
    val group: Group,
    val agentCount: Int,
)

interface GroupManagementUseCase {
    fun listGroups(): Flux<Group>

    fun listGroupsWithAgentCount(): Flux<GroupWithAgentCount>

    fun listGroupsPaged(
        page: Int,
        size: Int,
    ): Mono<PagedResult<GroupWithAgentCount>>

    fun createGroup(name: String): Mono<Group>

    fun updateGroup(
        id: String,
        name: String?,
        status: String?,
    ): Mono<Group>

    fun deleteGroup(id: String): Mono<Void>
}
