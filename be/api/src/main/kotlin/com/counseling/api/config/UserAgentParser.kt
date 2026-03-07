package com.counseling.api.config

import com.counseling.api.domain.CustomerDeviceInfo
import nl.basjes.parse.useragent.UserAgentAnalyzer
import org.springframework.stereotype.Component

@Component
class UserAgentParser {
    private val analyzer: UserAgentAnalyzer =
        UserAgentAnalyzer
            .newBuilder()
            .hideMatcherLoadStats()
            .withCache(5000)
            .withField("DeviceClass")
            .withField("DeviceBrand")
            .withField("OperatingSystemName")
            .withField("OperatingSystemVersion")
            .withField("AgentName")
            .withField("AgentVersion")
            .build()

    fun parse(userAgent: String?): CustomerDeviceInfo? {
        if (userAgent.isNullOrBlank()) return null
        val ua = analyzer.parse(userAgent)
        return CustomerDeviceInfo(
            deviceType = ua.getValue("DeviceClass").takeUnless { it == "Unknown" },
            deviceBrand = ua.getValue("DeviceBrand").takeUnless { it == "Unknown" },
            osName = ua.getValue("OperatingSystemName").takeUnless { it == "Unknown" },
            osVersion = ua.getValue("OperatingSystemVersion").takeUnless { it == "??" },
            browserName = ua.getValue("AgentName").takeUnless { it == "Unknown" },
            browserVersion = ua.getValue("AgentVersion").takeUnless { it == "??" },
            rawUserAgent = userAgent,
        )
    }
}
