package com.counseling.admin.adapter.inbound.web.controller

import com.counseling.admin.adapter.inbound.web.dto.CreateGroupRequest
import com.counseling.admin.adapter.inbound.web.dto.GroupResponse
import com.counseling.admin.adapter.inbound.web.dto.UpdateGroupRequest
import com.counseling.admin.port.inbound.GroupManagementUseCase
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
@RequestMapping("/api-adm/groups")
@Profile("!test")
class GroupController(
    private val groupManagementUseCase: GroupManagementUseCase,
) {
    @GetMapping
    fun listGroups(): Flux<GroupResponse> =
        groupManagementUseCase.listGroupsWithAgentCount().map { item ->
            GroupResponse(
                id = item.group.id,
                name = item.group.name,
                status = item.group.status.name,
                agentCount = item.agentCount,
                createdAt = item.group.createdAt,
                updatedAt = item.group.updatedAt,
            )
        }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createGroup(
        @RequestBody request: CreateGroupRequest,
    ): Mono<GroupResponse> =
        groupManagementUseCase.createGroup(request.name).map { group ->
            GroupResponse(
                id = group.id,
                name = group.name,
                status = group.status.name,
                agentCount = 0,
                createdAt = group.createdAt,
                updatedAt = group.updatedAt,
            )
        }

    @PutMapping("/{id}")
    fun updateGroup(
        @PathVariable id: UUID,
        @RequestBody request: UpdateGroupRequest,
    ): Mono<GroupResponse> =
        groupManagementUseCase.updateGroup(id, request.name, request.status).map { group ->
            GroupResponse(
                id = group.id,
                name = group.name,
                status = group.status.name,
                agentCount = 0,
                createdAt = group.createdAt,
                updatedAt = group.updatedAt,
            )
        }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteGroup(
        @PathVariable id: UUID,
    ): Mono<Void> = groupManagementUseCase.deleteGroup(id)
}
