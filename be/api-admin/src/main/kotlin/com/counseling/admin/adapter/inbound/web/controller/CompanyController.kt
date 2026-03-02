package com.counseling.admin.adapter.inbound.web.controller

import com.counseling.admin.adapter.inbound.web.dto.CompanyResponse
import com.counseling.admin.adapter.inbound.web.dto.UpdateCompanyRequest
import com.counseling.admin.port.inbound.CompanyManagementUseCase
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api-adm/company")
@Profile("!test")
class CompanyController(
    private val companyManagementUseCase: CompanyManagementUseCase,
) {
    @GetMapping
    fun getCompany(): Mono<CompanyResponse> =
        companyManagementUseCase.getCompany().map { company ->
            CompanyResponse(
                id = company.id,
                name = company.name,
                contact = company.contact,
                address = company.address,
                createdAt = company.createdAt,
                updatedAt = company.updatedAt,
            )
        }

    @PutMapping
    fun updateCompany(
        @RequestBody request: UpdateCompanyRequest,
    ): Mono<CompanyResponse> =
        companyManagementUseCase.updateCompany(request.name, request.contact, request.address).map { company ->
            CompanyResponse(
                id = company.id,
                name = company.name,
                contact = company.contact,
                address = company.address,
                createdAt = company.createdAt,
                updatedAt = company.updatedAt,
            )
        }
}
