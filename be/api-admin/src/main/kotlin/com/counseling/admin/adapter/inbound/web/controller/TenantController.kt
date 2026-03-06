package com.counseling.admin.adapter.inbound.web.controller

import com.counseling.admin.adapter.inbound.web.dto.CreateTenantRequest
import com.counseling.admin.adapter.inbound.web.dto.PageResponse
import com.counseling.admin.adapter.inbound.web.dto.TenantDetailResponse
import com.counseling.admin.adapter.inbound.web.dto.TenantSummaryResponse
import com.counseling.admin.adapter.inbound.web.dto.UpdateTenantRequest
import com.counseling.admin.adapter.inbound.web.dto.UpdateTenantStatusRequest
import com.counseling.admin.domain.TenantStatus
import com.counseling.admin.domain.exception.BadRequestException
import com.counseling.admin.port.inbound.CreateTenantCommand
import com.counseling.admin.port.inbound.TenantManagementUseCase
import com.counseling.admin.port.inbound.UpdateTenantCommand
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID
import kotlin.math.ceil

@RestController
@RequestMapping("/api-adm/tenants")
@Profile("!test")
class TenantController(
    private val tenantManagementUseCase: TenantManagementUseCase,
) {
    @GetMapping
    fun listTenants(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): Mono<PageResponse<TenantSummaryResponse>> =
        tenantManagementUseCase
            .listTenants(search, status, page, size)
            .map { result ->
                PageResponse(
                    content = result.content.map { TenantSummaryResponse.from(it) },
                    page = result.page,
                    size = result.size,
                    totalElements = result.totalElements,
                    totalPages = ceil(result.totalElements.toDouble() / result.size).toInt(),
                )
            }

    @GetMapping("/{id}")
    fun getTenant(
        @PathVariable id: UUID,
    ): Mono<TenantDetailResponse> =
        tenantManagementUseCase
            .getTenant(id)
            .map { TenantDetailResponse.from(it) }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createTenant(
        @RequestBody request: CreateTenantRequest,
    ): Mono<TenantDetailResponse> =
        tenantManagementUseCase
            .createTenant(
                CreateTenantCommand(
                    name = request.name,
                    slug = request.slug,
                    dbHost = request.dbHost,
                    dbPort = request.dbPort,
                    dbName = request.dbName,
                    dbUsername = request.dbUsername,
                    dbPassword = request.dbPassword,
                ),
            ).map { TenantDetailResponse.from(it) }

    @PutMapping("/{id}")
    fun updateTenant(
        @PathVariable id: UUID,
        @RequestBody request: UpdateTenantRequest,
    ): Mono<TenantDetailResponse> =
        tenantManagementUseCase
            .updateTenant(
                id,
                UpdateTenantCommand(
                    name = request.name,
                    dbHost = request.dbHost,
                    dbPort = request.dbPort,
                    dbName = request.dbName,
                    dbUsername = request.dbUsername,
                    dbPassword = request.dbPassword,
                ),
            ).map { TenantDetailResponse.from(it) }

    @PatchMapping("/{id}/status")
    fun updateTenantStatus(
        @PathVariable id: UUID,
        @RequestBody request: UpdateTenantStatusRequest,
    ): Mono<TenantDetailResponse> {
        val status =
            try {
                TenantStatus.valueOf(request.status)
            } catch (e: IllegalArgumentException) {
                return Mono.error(BadRequestException("Invalid tenant status: ${request.status}"))
            }
        return tenantManagementUseCase
            .updateTenantStatus(id, status)
            .map { TenantDetailResponse.from(it) }
    }
}
