package com.counseling.api.domain

data class CustomerDeviceInfo(
    val deviceType: String?,
    val deviceBrand: String?,
    val osName: String?,
    val osVersion: String?,
    val browserName: String?,
    val browserVersion: String?,
    val rawUserAgent: String?,
)
