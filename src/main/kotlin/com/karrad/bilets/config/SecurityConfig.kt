package com.karrad.bilets.config

import com.karrad.bilets.domain.repository.AuthTokenRepository
import com.karrad.bilets.domain.security.BearerTokenRateLimiter
import com.karrad.bilets.infrastructure.security.BearerTokenAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import java.time.Clock

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        authTokenRepository: AuthTokenRepository,
        bearerTokenRateLimiter: BearerTokenRateLimiter,
        clock: Clock
    ): SecurityFilterChain {
        val bearerFilter = BearerTokenAuthenticationFilter(authTokenRepository, bearerTokenRateLimiter, clock)

        http
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .addFilterBefore(bearerFilter, UsernamePasswordAuthenticationFilter::class.java)
            .authorizeHttpRequests { auth ->
                auth
                    // Аутентификация (публичные)
                    .requestMatchers(HttpMethod.POST, "/auth/send-code").permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()

                    // Venue preview (публичная HTML-страница)
                    .requestMatchers(HttpMethod.GET, "/").permitAll()

                    // Статические файлы (обложки мероприятий)
                    .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()

                    // Browsing (публичные GET)
                    .requestMatchers(HttpMethod.GET, "/api/v1/events", "/api/v1/events/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/categories", "/api/v1/categories/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/discovery").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/geo/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/organizations", "/api/v1/organizations/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/organization-applications", "/api/v1/organization-applications/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/venues", "/api/v1/venues/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/layout-templates", "/api/v1/layout-templates/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/inventory-plans", "/api/v1/inventory-plans/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/inventory", "/api/v1/inventory/**").permitAll()

                    // Платёжные callbacks (публичные — вызываются шлюзом)
                    .requestMatchers(HttpMethod.POST, "/api/v1/payments/callbacks/**").permitAll()

                    // Админский вход по паролю (без SMS)
                    .requestMatchers(HttpMethod.POST, "/admin/auth/login").permitAll()

                    // Инфраструктура
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                    .requestMatchers("/v3/api-docs", "/v3/api-docs/**").permitAll()

                    // Всё остальное требует аутентификации
                    .anyRequest().authenticated()
            }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint { _, response, _ ->
                    response.status = HttpStatus.UNAUTHORIZED.value()
                    response.contentType = MediaType.APPLICATION_JSON_VALUE
                    response.writer.write("""{"status":401,"detail":"Missing authorization: provide Bearer token"}""")
                }
                ex.accessDeniedHandler { _, response, _ ->
                    response.status = HttpStatus.FORBIDDEN.value()
                    response.contentType = MediaType.APPLICATION_JSON_VALUE
                    response.writer.write("""{"status":403,"detail":"Access denied"}""")
                }
            }
        return http.build()
    }
}
