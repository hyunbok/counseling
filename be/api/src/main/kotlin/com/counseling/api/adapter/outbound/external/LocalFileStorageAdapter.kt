package com.counseling.api.adapter.outbound.external

import com.counseling.api.port.outbound.FileStoragePort
import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.nio.file.Files
import java.nio.file.NoSuchFileException
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
                val filePath = Paths.get(path).normalize()
                val parent = filePath.parent ?: throw IllegalArgumentException("Invalid file path: $path")
                val normalizedParent = parent.normalize().toAbsolutePath()
                val normalizedFile = filePath.toAbsolutePath()
                if (!normalizedFile.startsWith(normalizedParent)) {
                    throw SecurityException("Path traversal attempt detected: $path")
                }
                Files.createDirectories(normalizedParent)
                Files.write(normalizedFile, content)
            }.subscribeOn(Schedulers.boundedElastic())
            .doOnError { e -> log.error("Failed to store file at {}: {}", path, e.message) }
            .then()

    override fun load(path: String): Mono<Resource> =
        Mono
            .fromCallable {
                val filePath = Paths.get(path).normalize().toAbsolutePath()
                if (!Files.exists(filePath)) {
                    throw NoSuchFileException(path)
                }
                FileSystemResource(filePath) as Resource
            }.subscribeOn(Schedulers.boundedElastic())

    override fun delete(path: String): Mono<Void> =
        Mono
            .fromCallable {
                val filePath = Paths.get(path).normalize().toAbsolutePath()
                Files.deleteIfExists(filePath)
            }.subscribeOn(Schedulers.boundedElastic())
            .doOnError { e -> log.error("Failed to delete file at {}: {}", path, e.message) }
            .then()
}
