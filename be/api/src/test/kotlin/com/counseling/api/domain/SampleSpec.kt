package com.counseling.api.domain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SampleSpec :
    StringSpec({
        "Kotest runner is wired correctly via JUnit 5 platform" {
            val result = 1 + 1
            result shouldBe 2
        }
    })
