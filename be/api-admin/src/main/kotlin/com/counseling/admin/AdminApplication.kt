package com.counseling.admin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class AdminApplication

fun main(args: Array<String>) {
    runApplication<AdminApplication>(*args)
}
