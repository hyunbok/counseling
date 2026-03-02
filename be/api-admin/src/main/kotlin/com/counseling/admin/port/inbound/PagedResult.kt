package com.counseling.admin.port.inbound

data class PagedResult<T>(
    val content: List<T>,
    val totalElements: Long,
    val page: Int,
    val size: Int,
)
