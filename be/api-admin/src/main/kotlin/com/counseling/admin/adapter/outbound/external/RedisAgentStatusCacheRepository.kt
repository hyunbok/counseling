package com.counseling.admin.adapter.outbound.external

import com.counseling.admin.port.outbound.AgentStatusCacheRepository
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
@Profile("!test")
class RedisAgentStatusCacheRepository(
    private val redisTemplate: ReactiveStringRedisTemplate,
) : AgentStatusCacheRepository {
    override fun getAgentStatuses(tenantSlug: String): Mono<Map<String, String>> =
        redisTemplate
            .keys("agent:status:$tenantSlug:*")
            .flatMap { key ->
                redisTemplate.opsForValue().get(key).map { value ->
                    val agentId = key.substringAfterLast(":")
                    agentId to value
                }
            }.collectMap({ it.first }, { it.second })
}
