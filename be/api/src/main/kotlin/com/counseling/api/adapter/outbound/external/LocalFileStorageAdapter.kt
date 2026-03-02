package com.counseling.api.adapter.outbound.external

import com.counseling.api.port.outbound.FileStoragePort
import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.nio.file.Files
import java.nio.file.Paths

@Component
class LocalFileStorageAdapter : FileStoragePort {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun store(
        path: String,
        content: ByteArray,
    ): Mono<Void> =
        Mono
            .fromCallable {
                val filePath = Paths.get(path)
                Files.createDirectories(filePath.parent)
                Files.write(filePath, content)
            }.subscribeOn(Schedulers.boundedElastic())
            .doOnError { e -> log.error("Failed to store file at {}: {}", path, e.message) }
            .then()

    override fun load(path: String): Mono<Resource> =
        Mono
            .fromCallable {
                val filePath = Paths.get(path)
                FileSystemResource(filePath) as Resource
            }.subscribeOn(Schedulers.boundedElastic())

    override fun delete(path: String): Mono<Void> =
        Mono
            .fromCallable {
                Files.deleteIfExists(Paths.get(path))
            }.subscribeOn(Schedulers.boundedElastic())
            .doOnError { e -> log.error("Failed to delete file at {}: {}", path, e.message) }
            .then()
}
