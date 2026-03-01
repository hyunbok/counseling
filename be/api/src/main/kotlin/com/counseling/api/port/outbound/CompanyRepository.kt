package com.counseling.api.port.outbound

import com.counseling.api.domain.Company
import reactor.core.publisher.Mono

interface CompanyRepository {
    fun save(company: Company): Mono<Company>

    fun findFirst(): Mono<Company>
}
