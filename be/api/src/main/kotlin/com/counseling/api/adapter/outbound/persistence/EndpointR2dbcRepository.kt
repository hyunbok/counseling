package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.Endpoint
import com.counseling.api.domain.EndpointType
import com.counseling.api.port.outbound.EndpointRepository
import io.r2dbc.spi.Readable
import org.springframework.context.annotation.Profile
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Repository
@Profile("!test")
class EndpointR2dbcRepository(
    private val databaseClient: DatabaseClient,
) : EndpointRepository {
    override fun save(endpoint: Endpoint): Mono<Endpoint> {
        val spec =
            databaseClient
                .sql(
                    """
                    INSERT INTO endpoints (id, channel_id, type, customer_name, customer_contact, joined_at, left_at)
                    VALUES (:id, :channelId, :type, :customerName, :customerContact, :joinedAt, :leftAt)
                    ON CONFLICT (id) DO UPDATE SET
                        left_at = :leftAt
                    """.trimIndent(),
                ).bind("id", endpoint.id)
                .bind("channelId", endpoint.channelId)
                .bind("type", endpoint.type.name)
                .bind("joinedAt", endpoint.joinedAt)
        val specWithName =
            if (endpoint.customerName != null) {
                spec.bind("customerName", endpoint.customerName)
            } else {
                spec.bindNull("customerName", String::class.java)
            }
        val specWithContact =
            if (endpoint.customerContact != null) {
                specWithName.bind("customerContact", endpoint.customerContact)
            } else {
                specWithName.bindNull("customerContact", String::class.java)
            }
        val specWithLeftAt =
            if (endpoint.leftAt != null) {
                specWithContact.bind("leftAt", endpoint.leftAt)
            } else {
                specWithContact.bindNull("leftAt", Instant::class.java)
            }
        return specWithLeftAt.then().thenReturn(endpoint)
    }

    override fun findAllByChannelId(channelId: UUID): Flux<Endpoint> =
        databaseClient
            .sql("SELECT * FROM endpoints WHERE channel_id = :channelId ORDER BY joined_at")
            .bind("channelId", channelId)
            .map { row -> mapToEndpoint(row) }
            .all()

    private fun mapToEndpoint(row: Readable): Endpoint =
        Endpoint(
            id = row.get("id", UUID::class.java)!!,
            channelId = row.get("channel_id", UUID::class.java)!!,
            type = EndpointType.valueOf(row.get("type", String::class.java)!!),
            customerName = row.get("customer_name", String::class.java),
            customerContact = row.get("customer_contact", String::class.java),
            joinedAt = row.get("joined_at", Instant::class.java)!!,
            leftAt = row.get("left_at", Instant::class.java),
        )
}
