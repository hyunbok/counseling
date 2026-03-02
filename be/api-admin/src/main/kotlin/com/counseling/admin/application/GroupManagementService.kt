package com.counseling.admin.application

import com.counseling.admin.domain.Group
import com.counseling.admin.domain.GroupStatus
import com.counseling.admin.domain.exception.ConflictException
import com.counseling.admin.domain.exception.NotFoundException
import com.counseling.admin.port.inbound.GroupManagementUseCase
import com.counseling.admin.port.inbound.GroupWithAgentCount
import com.counseling.admin.port.outbound.AdminAgentRepository
import com.counseling.admin.port.outbound.AdminGroupRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Service
@Profile("!test")
class GroupManagementService(
    private val groupRepository: AdminGroupRepository,
    private val agentRepository: AdminAgentRepository,
) : GroupManagementUseCase {
    override fun listGroups(): Flux<Group> = groupRepository.findAllByNotDeleted()

    override fun listGroupsWithAgentCount(): Flux<GroupWithAgentCount> =
        groupRepository
            .findAllByNotDeleted()
            .flatMap { group ->
                agentRepository
                    .countByGroupIdAndNotDeleted(group.id)
                    .map { count -> GroupWithAgentCount(group = group, agentCount = count.toInt()) }
            }

    override fun createGroup(name: String): Mono<Group> =
        groupRepository
            .findByNameAndNotDeleted(name)
            .flatMap<Group> { Mono.error(ConflictException("Group name already exists: $name")) }
            .switchIfEmpty(
                Mono.defer {
                    val now = Instant.now()
                    val group =
                        Group(
                            id = UUID.randomUUID(),
                            name = name,
                            status = GroupStatus.ACTIVE,
                            createdAt = now,
                            updatedAt = now,
                        )
                    groupRepository.save(group)
                },
            )

    override fun updateGroup(
        id: UUID,
        name: String?,
        status: String?,
    ): Mono<Group> =
        groupRepository
            .findByIdAndNotDeleted(id)
            .switchIfEmpty(Mono.error(NotFoundException("Group not found: $id")))
            .flatMap { group ->
                var updated = group
                if (name != null) updated = updated.rename(name)
                if (status != null) {
                    updated =
                        when (GroupStatus.valueOf(status)) {
                            GroupStatus.ACTIVE -> updated.activate()
                            GroupStatus.INACTIVE -> updated.deactivate()
                        }
                }
                groupRepository.save(updated)
            }

    override fun deleteGroup(id: UUID): Mono<Void> =
        groupRepository
            .findByIdAndNotDeleted(id)
            .switchIfEmpty(Mono.error(NotFoundException("Group not found: $id")))
            .flatMap { group ->
                groupRepository.save(group.softDelete())
            }.then()
}
