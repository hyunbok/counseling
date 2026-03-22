package com.counseling.api

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@Disabled("Requires DB connection for Spring Data R2DBC repositories")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class ApiApplicationTests {
    @Test
    fun contextLoads() {
    }
}
