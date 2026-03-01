package com.counseling.api.adapter.outbound.external

import com.counseling.api.port.outbound.TokenBlacklistRepository
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Duration

@Repository
@Profile("!test")
class RedisTokenBlacklistRepository(
    private val redisTemplate: ReactiveStringRedisTemplate,
) : TokenBlacklistRepository {
    companion object {
        private const val BLACKLIST_KEY_PREFIX = "auth:blacklist:"
    }

    override fun blacklist(
        jti: String,
        ttlSeconds: Long,
    ): Mono<Void> {
        val key = "$BLACKLIST_KEY_PREFIX$jti"
        return redisTemplate.opsForValue()
            .set(key, "1", Duration.ofSeconds(ttlSeconds))
            .then()
    }

    override fun isBlacklisted(jti: String): Mono<Boolean> {
        val key = "$BLACKLIST_KEY_PREFIX$jti"
        return redisTemplate.hasKey(key)
    }
}
