package com.karrad.bilets.infrastructure.security

import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.security.BearerTokenRateLimiter
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Clock

/**
 * Читает Bearer-токен из заголовка Authorization, проверяет в БД и помещает
 * UsernamePasswordAuthenticationToken в SecurityContextHolder.
 *
 * Логика:
 * - Нет заголовка → continue без SecurityContext (публичные роуты пройдут, защищённые получат 401 от Spring Security)
 * - Токен не найден в БД → rate limit по IP, ответ 401
 * - Токен истёк → ответ 401 (без rate limit — не попытка взлома)
 * - Токен валиден → SecurityContext заполнен, continue
 */
class BearerTokenAuthenticationFilter(
    private val authTokenRepository: AuthTokenRepository,
    private val bearerTokenRateLimiter: BearerTokenRateLimiter,
    private val clock: Clock
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response)
            return
        }

        val token = authHeader.removePrefix("Bearer ").trim()
        val authToken = authTokenRepository.findByToken(token)

        if (authToken == null) {
            val ip = request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
                ?: request.remoteAddr
            val tooMany = bearerTokenRateLimiter.recordFailure(ip)
            val status = if (tooMany) HttpStatus.TOO_MANY_REQUESTS else HttpStatus.UNAUTHORIZED
            sendError(response, status, if (tooMany) "Too many invalid token attempts" else "Invalid or expired token")
            return
        }

        if (authToken.isExpired(clock.instant())) {
            sendError(response, HttpStatus.UNAUTHORIZED, "Token has expired")
            return
        }

        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
        val authentication = UsernamePasswordAuthenticationToken(authToken.userId, null, authorities)
        SecurityContextHolder.getContext().authentication = authentication
        chain.doFilter(request, response)
    }

    private fun sendError(response: HttpServletResponse, status: HttpStatus, detail: String) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write("""{"status":${status.value()},"detail":"$detail"}""")
    }
}
