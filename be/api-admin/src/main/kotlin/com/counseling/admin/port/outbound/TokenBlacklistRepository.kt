package com.counseling.admin.port.outbound

import reactor.core.publisher.Mono

interface TokenBlacklistRepository {
    fun blacklist(
        jti: String,
        ttlSeconds: Long,
    ): Mono<Void>

    fun isBlacklisted(jti: String): Mono<Boolean>
}
