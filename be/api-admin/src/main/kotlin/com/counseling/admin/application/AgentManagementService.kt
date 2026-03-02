package com.counseling.admin.application

import com.counseling.admin.domain.Agent
import com.counseling.admin.domain.AgentRole
import com.counseling.admin.domain.AgentStatus
import com.counseling.admin.domain.exception.ConflictException
import com.counseling.admin.domain.exception.NotFoundException
import com.counseling.admin.port.inbound.AgentManagementUseCase
import com.counseling.admin.port.inbound.CreateAgentCommand
import com.counseling.admin.port.inbound.CreateAgentResult
import com.counseling.admin.port.inbound.UpdateAgentCommand
import com.counseling.admin.port.outbound.AdminAgentRepository
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Instant
import java.util.UUID

@Service
@Profile("!test")
class AgentManagementService(
    private val agentRepository: AdminAgentRepository,
    private val passwordEncoder: PasswordEncoder,
) : AgentManagementUseCase {
    override fun listAgents(groupId: UUID?): Flux<Agent> =
        if (groupId != null) {
            agentRepository.findAllByGroupIdAndNotDeleted(groupId)
        } else {
            agentRepository.findAllByNotDeleted()
        }

    override fun getAgent(id: UUID): Mono<Agent> =
        agentRepository
            .findByIdAndNotDeleted(id)
            .switchIfEmpty(Mono.error(NotFoundException("Agent not found: $id")))

    override fun createAgent(command: CreateAgentCommand): Mono<CreateAgentResult> =
        agentRepository
            .findByUsernameAndNotDeleted(command.username)
            .flatMap<CreateAgentResult> {
                Mono.error(ConflictException("Username already exists: ${command.username}"))
            }.switchIfEmpty(
                Mono.defer {
                    val tempPassword = PasswordGenerator.generate()
                    Mono
                        .fromCallable { passwordEncoder.encode(tempPassword)!! }
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap { hash ->
                            val now = Instant.now()
                            val agent =
                                Agent(
                                    id = UUID.randomUUID(),
                                    username = command.username,
                                    passwordHash = hash,
                                    name = command.name,
                                    role = AgentRole.valueOf(command.role),
                                    agentStatus = AgentStatus.OFFLINE,
                                    groupId = command.groupId,
                                    active = true,
                                    createdAt = now,
                                    updatedAt = now,
                                )
                            agentRepository.save(agent).map { saved ->
                                CreateAgentResult(
                                    agent = saved,
                                    temporaryPassword = tempPassword,
                                )
                            }
                        }
                },
            )

    override fun updateAgent(
        id: UUID,
        command: UpdateAgentCommand,
    ): Mono<Agent> =
        agentRepository
            .findByIdAndNotDeleted(id)
            .switchIfEmpty(Mono.error(NotFoundException("Agent not found: $id")))
            .flatMap { agent ->
                var updated = agent
                if (command.name != null) updated = updated.copy(name = command.name, updatedAt = Instant.now())
                if (command.role != null) {
                    updated = updated.copy(role = AgentRole.valueOf(command.role), updatedAt = Instant.now())
                }
                if (command.groupId != null) updated = updated.assignToGroup(command.groupId)
                agentRepository.save(updated)
            }

    override fun toggleAgentActive(
        id: UUID,
        active: Boolean,
    ): Mono<Agent> =
        agentRepository
            .findByIdAndNotDeleted(id)
            .switchIfEmpty(Mono.error(NotFoundException("Agent not found: $id")))
            .flatMap { agent ->
                val updated = if (active) agent.activate() else agent.deactivate()
                agentRepository.save(updated)
            }

    override fun resetPassword(id: UUID): Mono<String> =
        agentRepository
            .findByIdAndNotDeleted(id)
            .switchIfEmpty(Mono.error(NotFoundException("Agent not found: $id")))
            .flatMap { agent ->
                val tempPassword = PasswordGenerator.generate()
                Mono
                    .fromCallable { passwordEncoder.encode(tempPassword)!! }
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap { hash ->
                        agentRepository.save(agent.changePassword(hash)).thenReturn(tempPassword)
                    }
            }
}
