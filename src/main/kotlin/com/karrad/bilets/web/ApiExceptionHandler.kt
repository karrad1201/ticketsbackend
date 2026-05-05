package com.karrad.bilets.web

import com.karrad.bilets.application.usecase.OtpBruteForceException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(exception: IllegalArgumentException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, sanitize(exception.message) ?: "Bad request")
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(exception: IllegalStateException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, sanitize(exception.message) ?: "Conflict")
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(exception: NoSuchElementException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, sanitize(exception.message) ?: "Not found")
    }

    @ExceptionHandler(SecurityException::class)
    fun handleForbidden(exception: SecurityException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, sanitize(exception.message) ?: "Forbidden")
    }

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbidden(exception: ForbiddenException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, sanitize(exception.message) ?: "Forbidden")
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(exception: UnauthorizedException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, sanitize(exception.message) ?: "Unauthorized")
    }

    @ExceptionHandler(TooManyRequestsException::class)
    fun handleTooManyRequests(exception: TooManyRequestsException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, sanitize(exception.message) ?: "Too many requests")
    }

    @ExceptionHandler(OtpBruteForceException::class)
    fun handleOtpBruteForce(exception: OtpBruteForceException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, sanitize(exception.message) ?: "Too many attempts")
    }

    /** Удаляет PII из сообщений об ошибках перед отправкой клиенту. */
    internal fun sanitize(message: String?): String? {
        if (message == null) return null
        return message
            .replace(Regex("""\+\d{10,13}"""), "[phone]")
            .replace(Regex("""[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}"""), "[email]")
            .replace(Regex("""[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"""), "[id]")
    }
}
