package com.counseling.api

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Disabled("Spring Data R2DBC repositories require ConnectionFactory — use integration test profile")
class ApiApplicationTests {
    @Test
    fun contextLoads() {
    }
}
