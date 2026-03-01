package com.counseling.api.adapter.inbound.web

import com.counseling.api.adapter.inbound.web.dto.ErrorResponse
import com.counseling.api.domain.exception.BadRequestException
import com.counseling.api.domain.exception.ConflictException
import com.counseling.api.domain.exception.NotFoundException
import com.counseling.api.domain.exception.UnauthorizedException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ServerWebExchange

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(UnauthorizedException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleUnauthorized(
        ex: UnauthorizedException,
        exchange: ServerWebExchange,
    ): ErrorResponse =
        ErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            error = HttpStatus.UNAUTHORIZED.reasonPhrase,
            message = ex.message ?: "Unauthorized",
            path = exchange.request.uri.path,
        )

    @ExceptionHandler(BadRequestException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleBadRequest(
        ex: BadRequestException,
        exchange: ServerWebExchange,
    ): ErrorResponse =
        ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = ex.message ?: "Bad request",
            path = exchange.request.uri.path,
        )

    @ExceptionHandler(NotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(
        ex: NotFoundException,
        exchange: ServerWebExchange,
    ): ErrorResponse =
        ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            error = HttpStatus.NOT_FOUND.reasonPhrase,
            message = ex.message ?: "Not found",
            path = exchange.request.uri.path,
        )

    @ExceptionHandler(ConflictException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleConflict(
        ex: ConflictException,
        exchange: ServerWebExchange,
    ): ErrorResponse =
        ErrorResponse(
            status = HttpStatus.CONFLICT.value(),
            error = HttpStatus.CONFLICT.reasonPhrase,
            message = ex.message ?: "Conflict",
            path = exchange.request.uri.path,
        )
}
