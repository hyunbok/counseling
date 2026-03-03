package com.counseling.api.config

import com.mongodb.ReadPreference
import com.mongodb.WriteConcern
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!test")
class MongoConfig {
    @Bean
    fun mongoClientSettingsBuilderCustomizer(): MongoClientSettingsBuilderCustomizer =
        MongoClientSettingsBuilderCustomizer { builder ->
            // secondaryPreferred: distributes reads across replicas in production.
            // Falls back to primary on single-node (local dev) — safe for all environments.
            builder
                .readPreference(ReadPreference.secondaryPreferred())
                .writeConcern(WriteConcern.MAJORITY)
        }
}
