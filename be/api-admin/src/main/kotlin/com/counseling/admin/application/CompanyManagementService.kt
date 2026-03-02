package com.counseling.admin.application

import com.counseling.admin.domain.Company
import com.counseling.admin.domain.exception.NotFoundException
import com.counseling.admin.port.inbound.CompanyManagementUseCase
import com.counseling.admin.port.outbound.AdminCompanyRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
@Profile("!test")
class CompanyManagementService(
    private val companyRepository: AdminCompanyRepository,
) : CompanyManagementUseCase {
    override fun getCompany(): Mono<Company> =
        companyRepository
            .findFirst()
            .switchIfEmpty(Mono.error(NotFoundException("Company not found")))

    override fun updateCompany(
        name: String,
        contact: String?,
        address: String?,
    ): Mono<Company> =
        companyRepository
            .findFirst()
            .switchIfEmpty(Mono.error(NotFoundException("Company not found")))
            .flatMap { company ->
                companyRepository.save(company.update(name, contact, address))
            }
}
