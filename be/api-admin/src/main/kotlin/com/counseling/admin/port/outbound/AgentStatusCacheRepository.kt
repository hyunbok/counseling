package com.counseling.admin.port.outbound

import reactor.core.publisher.Mono

interface AgentStatusCacheRepository {
    fun getAgentStatuses(tenantSlug: String): Mono<Map<String, String>>
}
