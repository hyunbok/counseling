package com.counseling.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.data.r2dbc.autoconfigure.DataR2dbcAutoConfiguration
import org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(
    exclude = [
        R2dbcAutoConfiguration::class,
        DataR2dbcAutoConfiguration::class,
    ],
)
@ConfigurationPropertiesScan
class ApiApplication

fun main(args: Array<String>) {
    runApplication<ApiApplication>(*args)
}
