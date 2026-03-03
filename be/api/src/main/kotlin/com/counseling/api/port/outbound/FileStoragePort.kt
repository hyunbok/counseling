package com.counseling.api.port.outbound

import org.springframework.core.io.Resource
import reactor.core.publisher.Mono

interface FileStoragePort {
    fun store(
        path: String,
        content: ByteArray,
    ): Mono<Void>

    fun load(path: String): Mono<Resource>

    fun delete(path: String): Mono<Void>
}
