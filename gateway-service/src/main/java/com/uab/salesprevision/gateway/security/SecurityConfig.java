package com.uab.salesprevision.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * This service only validates and routes — it holds no key material and
 * issues no tokens. Tokens are verified against user-service's public key,
 * fetched from {@code spring.security.oauth2.resourceserver.jwt.jwk-set-uri}
 * (application.properties) and auto-configured by Spring Boot, exactly like
 * every other resource server in this codebase.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(auth -> auth
                        // Login is the one endpoint that must be reachable pre-auth
                        .pathMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        // Public key discovery for resource servers
                        .pathMatchers("/.well-known/jwks.json").permitAll()
                        // Actuator
                        .pathMatchers("/actuator/**").permitAll()
                        // Circuit-breaker fallback
                        .pathMatchers("/fallback/**").permitAll()
                        // Swagger UI + OpenAPI docs
                        .pathMatchers(
                                "/swagger-ui.html", "/swagger-ui/**",
                                "/webjars/**", "/v3/api-docs/**"
                        ).permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }
}
