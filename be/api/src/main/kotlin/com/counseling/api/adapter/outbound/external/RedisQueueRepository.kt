package com.counseling.api.adapter.outbound.external

import com.counseling.api.domain.QueueEntry
import com.counseling.api.domain.exception.NotFoundException
import com.counseling.api.port.outbound.QueueRepository
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule

@Repository
@Profile("!test")
class RedisQueueRepository(
    private val redisTemplate: ReactiveStringRedisTemplate,
) : QueueRepository {
    private val mapper =
        JsonMapper
            .builder()
            .addModule(KotlinModule.Builder().build())
            .build()

    companion object {
        private fun sortedSetKey(tenantId: String) = "queue:$tenantId"

        private fun hashKey(tenantId: String) = "queue-idx:$tenantId"

        private val REMOVE_ATOMICALLY_SCRIPT =
            RedisScript.of(
                """
                local hashKey = KEYS[1]
                local zsetKey = KEYS[2]
                local entryId = ARGV[1]
                local json = redis.call('HGET', hashKey, entryId)
                if not json then
                    return nil
                end
                redis.call('ZREM', zsetKey, json)
                redis.call('HDEL', hashKey, entryId)
                return json
                """.trimIndent(),
                String::class.java,
            )
    }

    private fun serialize(entry: QueueEntry): String = mapper.writeValueAsString(entry)

    private fun deserialize(json: String): QueueEntry = mapper.readValue(json, QueueEntry::class.java)

    override fun add(
        tenantId: String,
        entry: QueueEntry,
    ): Mono<Boolean> {
        val zsetKey = sortedSetKey(tenantId)
        val hKey = hashKey(tenantId)
        val json = serialize(entry)
        val score = entry.enteredAt.toEpochMilli().toDouble()

        return redisTemplate
            .opsForZSet()
            .add(zsetKey, json, score)
            .flatMap { added ->
                redisTemplate
                    .opsForHash<String, String>()
                    .put(hKey, entry.id, json)
                    .thenReturn(added)
            }
    }

    override fun remove(
        tenantId: String,
        entryId: String,
    ): Mono<QueueEntry> {
        val hKey = hashKey(tenantId)
        val zsetKey = sortedSetKey(tenantId)

        return redisTemplate
            .opsForHash<String, String>()
            .get(hKey, entryId)
            .flatMap { json ->
                redisTemplate
                    .opsForZSet()
                    .remove(zsetKey, json)
                    .then(
                        redisTemplate
                            .opsForHash<String, String>()
                            .remove(hKey, entryId),
                    ).thenReturn(deserialize(json))
            }
    }

    override fun findAll(tenantId: String): Flux<QueueEntry> {
        val zsetKey = sortedSetKey(tenantId)
        return redisTemplate
            .opsForZSet()
            .range(
                zsetKey,
                org.springframework.data.domain.Range
                    .unbounded(),
            ).map { deserialize(it) }
    }

    override fun findById(
        tenantId: String,
        entryId: String,
    ): Mono<QueueEntry> {
        val hKey = hashKey(tenantId)
        return redisTemplate
            .opsForHash<String, String>()
            .get(hKey, entryId)
            .switchIfEmpty(Mono.error(NotFoundException("Queue entry not found: $entryId")))
            .map { deserialize(it) }
    }

    override fun getPosition(
        tenantId: String,
        entryId: String,
    ): Mono<Long> {
        val hKey = hashKey(tenantId)
        val zsetKey = sortedSetKey(tenantId)

        return redisTemplate
            .opsForHash<String, String>()
            .get(hKey, entryId)
            .switchIfEmpty(Mono.error(NotFoundException("Queue entry not found: $entryId")))
            .flatMap { json ->
                redisTemplate
                    .opsForZSet()
                    .rank(zsetKey, json)
                    .map { rank -> rank + 1 }
            }
    }

    override fun getSize(tenantId: String): Mono<Long> {
        val zsetKey = sortedSetKey(tenantId)
        return redisTemplate.opsForZSet().size(zsetKey)
    }

    override fun removeAtomically(
        tenantId: String,
        entryId: String,
    ): Mono<QueueEntry> {
        val hKey = hashKey(tenantId)
        val zsetKey = sortedSetKey(tenantId)

        return redisTemplate
            .execute(
                REMOVE_ATOMICALLY_SCRIPT,
                listOf(hKey, zsetKey),
                listOf(entryId),
            ).next()
            .switchIfEmpty(Mono.error(NotFoundException("Queue entry not found: $entryId")))
            .map { deserialize(it) }
    }
}
