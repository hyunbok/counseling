package com.counseling.admin.adapter.outbound.persistence

import com.counseling.admin.domain.Company
import com.counseling.admin.port.outbound.AdminCompanyRepository
import org.springframework.context.annotation.Profile
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Repository
@Profile("!test")
class AdminCompanyR2dbcRepository(
    private val databaseClient: DatabaseClient,
) : AdminCompanyRepository {
    override fun save(company: Company): Mono<Company> =
        databaseClient
            .sql(
                """
                INSERT INTO companies (id, name, contact, address, created_at, updated_at)
                VALUES (:id, :name, :contact, :address, :createdAt, :updatedAt)
                ON CONFLICT (id) DO UPDATE SET
                    name = :name, contact = :contact, address = :address, updated_at = :updatedAt
                """.trimIndent(),
            ).bind("id", company.id)
            .bind("name", company.name)
            .bindNullable("contact", company.contact, String::class.java)
            .bindNullable("address", company.address, String::class.java)
            .bind("createdAt", company.createdAt)
            .bind("updatedAt", company.updatedAt)
            .then()
            .thenReturn(company)

    override fun findFirst(): Mono<Company> =
        databaseClient
            .sql("SELECT * FROM companies LIMIT 1")
            .map { row ->
                Company(
                    id = row.get("id", UUID::class.java)!!,
                    name = row.get("name", String::class.java)!!,
                    contact = row.get("contact", String::class.java),
                    address = row.get("address", String::class.java),
                    createdAt = row.get("created_at", Instant::class.java)!!,
                    updatedAt = row.get("updated_at", Instant::class.java)!!,
                )
            }.one()

    private fun DatabaseClient.GenericExecuteSpec.bindNullable(
        name: String,
        value: Any?,
        type: Class<*>,
    ): DatabaseClient.GenericExecuteSpec =
        if (value != null) {
            this.bind(name, value)
        } else {
            this.bindNull(name, type)
        }
}
