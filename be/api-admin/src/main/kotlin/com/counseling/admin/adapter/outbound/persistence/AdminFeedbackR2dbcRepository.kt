package com.counseling.admin.adapter.outbound.persistence

import com.counseling.admin.domain.Feedback
import com.counseling.admin.port.outbound.AdminFeedbackRepository
import org.springframework.context.annotation.Profile
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Repository
@Profile("!test")
class AdminFeedbackR2dbcRepository(
    private val databaseClient: DatabaseClient,
) : AdminFeedbackRepository {
    override fun findById(id: UUID): Mono<Feedback> =
        databaseClient
            .sql("SELECT * FROM feedbacks WHERE id = :id")
            .bind("id", id)
            .map { row -> mapToFeedback(row) }
            .one()

    override fun findAll(
        agentId: UUID?,
        rating: Int?,
        page: Int,
        size: Int,
    ): Flux<Feedback> {
        val conditions = mutableListOf<String>()
        val params = mutableMapOf<String, Any>()

        if (agentId != null) {
            conditions.add("f.channel_id IN (SELECT id FROM channels WHERE agent_id = :agentId)")
            params["agentId"] = agentId
        }
        if (rating != null) {
            conditions.add("f.rating = :rating")
            params["rating"] = rating
        }

        val whereClause = if (conditions.isNotEmpty()) "WHERE ${conditions.joinToString(" AND ")}" else ""
        val sql = "SELECT f.* FROM feedbacks f $whereClause ORDER BY f.created_at DESC LIMIT :limit OFFSET :offset"
        params["limit"] = size
        params["offset"] = page * size

        var spec = databaseClient.sql(sql)
        params.forEach { (key, value) -> spec = spec.bind(key, value) }

        return spec.map { row -> mapToFeedback(row) }.all()
    }

    override fun countAll(
        agentId: UUID?,
        rating: Int?,
    ): Mono<Long> {
        val conditions = mutableListOf<String>()
        val params = mutableMapOf<String, Any>()

        if (agentId != null) {
            conditions.add("f.channel_id IN (SELECT id FROM channels WHERE agent_id = :agentId)")
            params["agentId"] = agentId
        }
        if (rating != null) {
            conditions.add("f.rating = :rating")
            params["rating"] = rating
        }

        val whereClause = if (conditions.isNotEmpty()) "WHERE ${conditions.joinToString(" AND ")}" else ""
        val sql = "SELECT COUNT(*) as cnt FROM feedbacks f $whereClause"

        var spec = databaseClient.sql(sql)
        params.forEach { (key, value) -> spec = spec.bind(key, value) }

        return spec
            .map { row -> row.get("cnt", java.lang.Long::class.java)!!.toLong() }
            .one()
            .defaultIfEmpty(0L)
    }

    private fun mapToFeedback(row: io.r2dbc.spi.Readable): Feedback =
        Feedback(
            id = row.get("id", UUID::class.java)!!,
            channelId = row.get("channel_id", UUID::class.java)!!,
            rating = row.get("rating", Integer::class.java)!!.toInt(),
            comment = row.get("comment", String::class.java),
            createdAt = row.get("created_at", Instant::class.java)!!,
        )
}
