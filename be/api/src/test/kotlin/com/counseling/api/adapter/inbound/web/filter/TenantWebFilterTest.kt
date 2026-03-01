package com.counseling.api.adapter.inbound.web.filter

import com.counseling.api.domain.TenantContext
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class TenantWebFilterTest :
    StringSpec({
        val filter = TenantWebFilter()

        fun chain(capturedTenantId: MutableList<String>): WebFilterChain {
            val chain = mockk<WebFilterChain>()
            every { chain.filter(any()) } answers {
                val exchange = firstArg<org.springframework.web.server.ServerWebExchange>()
                TenantContext
                    .getTenantId()
                    .doOnNext { capturedTenantId.add(it) }
                    .then(Mono.empty())
            }
            return chain
        }

        "filter injects tenant ID from header into context" {
            val captured = mutableListOf<String>()
            val request =
                MockServerHttpRequest
                    .get("/api/tenants")
                    .header(TenantWebFilter.TENANT_HEADER, "acme")
                    .build()
            val exchange = MockServerWebExchange.from(request)
            val chain = chain(captured)

            StepVerifier
                .create(filter.filter(exchange, chain))
                .verifyComplete()

            captured shouldBe listOf("acme")
        }

        "filter returns 400 for API path without tenant header" {
            val chain = mockk<WebFilterChain>()
            val request =
                MockServerHttpRequest
                    .get("/api/tenants")
                    .build()
            val exchange = MockServerWebExchange.from(request)

            StepVerifier
                .create(filter.filter(exchange, chain))
                .verifyComplete()

            exchange.response.statusCode shouldBe HttpStatus.BAD_REQUEST
        }

        "filter skips tenant check for actuator paths" {
            val chain = mockk<WebFilterChain>()
            every { chain.filter(any()) } returns Mono.empty()
            val request =
                MockServerHttpRequest
                    .get("/actuator/health")
                    .build()
            val exchange = MockServerWebExchange.from(request)

            StepVerifier
                .create(filter.filter(exchange, chain))
                .verifyComplete()

            exchange.response.statusCode shouldBe null
        }

        "filter skips tenant check for super-admin paths" {
            val chain = mockk<WebFilterChain>()
            every { chain.filter(any()) } returns Mono.empty()
            val request =
                MockServerHttpRequest
                    .get("/api/super-admin/tenants")
                    .build()
            val exchange = MockServerWebExchange.from(request)

            StepVerifier
                .create(filter.filter(exchange, chain))
                .verifyComplete()

            exchange.response.statusCode shouldBe null
        }
    })
