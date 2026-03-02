package com.counseling.admin.port.inbound

import com.counseling.admin.domain.Company
import reactor.core.publisher.Mono

interface CompanyManagementUseCase {
    fun getCompany(): Mono<Company>

    fun updateCompany(
        name: String,
        contact: String?,
        address: String?,
    ): Mono<Company>
}
