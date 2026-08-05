package com.uab.core.exception;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Registers {@link GlobalExceptionHandler} explicitly. Each service's
 * {@code @SpringBootApplication} only component-scans its own base package
 * (e.g. {@code com.uab.salesprevision.template}), never {@code com.uab.core} —
 * so without this, the shared exception handler is silently never picked up
 * and every service falls back to Spring's default (undocumented) error shape.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class GlobalExceptionHandlerAutoConfiguration {

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
