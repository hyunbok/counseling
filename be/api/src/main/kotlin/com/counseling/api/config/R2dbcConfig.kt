package com.counseling.api.config

import com.counseling.api.adapter.outbound.persistence.TenantRoutingConnectionFactory
import com.counseling.api.port.outbound.TenantConnectionRegistry
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.binding.BindMarkersFactory
import java.time.Duration

@Configuration
@Profile("!test")
class R2dbcConfig {
    @Bean
    @Qualifier("metaConnectionFactory")
    fun metaConnectionFactory(
        @Value("\${spring.r2dbc.url}") url: String,
        @Value("\${spring.r2dbc.username}") username: String,
        @Value("\${spring.r2dbc.password}") password: String,
        @Value("\${spring.r2dbc.pool.initial-size:2}") initialSize: Int,
        @Value("\${spring.r2dbc.pool.max-size:20}") maxSize: Int,
    ): ConnectionPool {
        val options =
            ConnectionFactoryOptions
                .parse(url)
                .mutate()
                .option(ConnectionFactoryOptions.USER, username)
                .option(ConnectionFactoryOptions.PASSWORD, password)
                .build()

        val connectionFactory = ConnectionFactories.get(options)

        val poolConfig =
            ConnectionPoolConfiguration
                .builder(connectionFactory)
                .maxSize(maxSize)
                .initialSize(initialSize)
                .maxIdleTime(Duration.ofMinutes(10))
                .build()

        return ConnectionPool(poolConfig)
    }

    @Bean
    @Qualifier("metaDatabaseClient")
    fun metaDatabaseClient(
        @Qualifier("metaConnectionFactory") connectionFactory: ConnectionFactory,
    ): DatabaseClient = DatabaseClient.create(connectionFactory)

    @Bean
    @Primary
    fun tenantConnectionFactory(connectionRegistry: TenantConnectionRegistry): ConnectionFactory =
        TenantRoutingConnectionFactory(connectionRegistry)

    @Bean
    @Primary
    fun tenantDatabaseClient(
        @Qualifier("tenantConnectionFactory") connectionFactory: ConnectionFactory,
    ): DatabaseClient =
        DatabaseClient
            .builder()
            .connectionFactory(connectionFactory)
            .bindMarkers(BindMarkersFactory.indexed("$", 1))
            .build()
}
