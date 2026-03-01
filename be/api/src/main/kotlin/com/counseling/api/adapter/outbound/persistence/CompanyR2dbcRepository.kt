package com.counseling.api.adapter.outbound.persistence

import com.counseling.api.domain.Company
import com.counseling.api.port.outbound.CompanyRepository
import io.r2dbc.spi.Readable
import org.springframework.context.annotation.Profile
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.UUID

@Repository
@Profile("!test")
class CompanyR2dbcRepository(
    private val databaseClient: DatabaseClient,
) : CompanyRepository {
    override fun save(company: Company): Mono<Company> {
        val spec =
            databaseClient
                .sql(
                    """
                    INSERT INTO companies (id, name, contact, address, created_at, updated_at)
                    VALUES (:id, :name, :contact, :address, :createdAt, :updatedAt)
                    ON CONFLICT (id) DO UPDATE SET
                        name = :name,
                        contact = :contact,
                        address = :address,
                        updated_at = :updatedAt
                    """.trimIndent(),
                ).bind("id", company.id)
                .bind("name", company.name)
                .bind("createdAt", company.createdAt)
                .bind("updatedAt", company.updatedAt)
        val specWithContact =
            if (company.contact != null) {
                spec.bind("contact", company.contact)
            } else {
                spec.bindNull("contact", String::class.java)
            }
        val specWithAddress =
            if (company.address != null) {
                specWithContact.bind("address", company.address)
            } else {
                specWithContact.bindNull("address", String::class.java)
            }
        return specWithAddress.then().thenReturn(company)
    }

    override fun findFirst(): Mono<Company> =
        databaseClient
            .sql("SELECT * FROM companies LIMIT 1")
            .map { row -> mapToCompany(row) }
            .one()

    private fun mapToCompany(row: Readable): Company =
        Company(
            id = row.get("id", UUID::class.java)!!,
            name = row.get("name", String::class.java)!!,
            contact = row.get("contact", String::class.java),
            address = row.get("address", String::class.java),
            createdAt = row.get("created_at", Instant::class.java)!!,
            updatedAt = row.get("updated_at", Instant::class.java)!!,
        )
}
