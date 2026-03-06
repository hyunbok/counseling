plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    id("org.springframework.boot") version "4.0.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
}

group = "com.counseling"
version = "0.0.1-SNAPSHOT"

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // Spring WebFlux
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Spring Security
    implementation("org.springframework.boot:spring-boot-starter-security")

    // R2DBC (PostgreSQL, reactive)
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    runtimeOnly("org.postgresql:r2dbc-postgresql")

    // Flyway (DB migration — uses JDBC)
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // MongoDB Reactive
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")

    // Redis Reactive
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
    implementation("org.apache.commons:commons-pool2:2.12.0")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // LiveKit Server SDK
    implementation("io.livekit:livekit-server:0.6.2")

    // JSON (Jackson 3.x — Spring Boot 4)
    implementation("tools.jackson.module:jackson-module-kotlin")

    // Mail
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // Actuator
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Test — JUnit 5 (Spring integration tests)
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.mockito")
    }
    testImplementation("io.projectreactor:reactor-test")

    // Test — Kotest (pure unit tests, no Spring extension)
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")

    // Test — MockK
    testImplementation("io.mockk:mockk:1.13.13")
}

ktlint {
    version.set("1.5.0")
    outputColorName.set("RED")
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
