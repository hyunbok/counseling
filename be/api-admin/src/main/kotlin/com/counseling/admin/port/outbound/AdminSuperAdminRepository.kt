package com.counseling.admin.port.outbound

import com.counseling.admin.domain.SuperAdmin
import reactor.core.publisher.Mono

interface AdminSuperAdminRepository {
    fun findByUsernameAndNotDeleted(username: String): Mono<SuperAdmin>
}
