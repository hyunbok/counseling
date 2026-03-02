package com.counseling.api.application

import com.counseling.api.domain.SharedFile
import com.counseling.api.port.inbound.SharedFileListResult
import com.counseling.api.port.inbound.SharedFileQuery
import com.counseling.api.port.outbound.FileNotificationPort
import com.counseling.api.port.outbound.SharedFileReadRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Service
@Profile("!test")
class SharedFileQueryService(
    private val sharedFileReadRepository: SharedFileReadRepository,
    private val fileNotificationPort: FileNotificationPort,
) : SharedFileQuery {
    override fun listFiles(
        channelId: UUID,
        before: Instant?,
        limit: Int,
    ): Mono<SharedFileListResult> =
        sharedFileReadRepository
            .findByChannelId(channelId, before, limit + 1)
            .collectList()
            .map { files ->
                val hasMore = files.size > limit
                val trimmed = if (hasMore) files.dropLast(1) else files
                val reversed = trimmed.reversed()
                SharedFileListResult(
                    files = reversed,
                    hasMore = hasMore,
                    oldestTimestamp = reversed.firstOrNull()?.createdAt,
                )
            }

    override fun streamFileEvents(channelId: UUID): Flux<SharedFile> = fileNotificationPort.subscribeFiles(channelId)
}
