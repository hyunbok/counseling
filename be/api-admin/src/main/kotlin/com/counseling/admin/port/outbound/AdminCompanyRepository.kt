package com.counseling.admin.port.outbound

import com.counseling.admin.domain.Company
import reactor.core.publisher.Mono

interface AdminCompanyRepository {
    fun save(company: Company): Mono<Company>

    fun findFirst(): Mono<Company>
}
