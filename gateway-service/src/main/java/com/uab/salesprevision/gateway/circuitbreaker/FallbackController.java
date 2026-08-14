package com.uab.salesprevision.gateway.circuitbreaker;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
@Tag(name = "Fallback", description = "Circuit breaker fallback responses — never called directly, only reached when a proxied service is unavailable.")
public class FallbackController {

    @RequestMapping("/service-unavailable")
    @Operation(summary = "Circuit breaker fallback", description = "Returned by the gateway instead of proxying, when the target service's circuit breaker is open.")
    public Mono<ResponseEntity<Map<String, Object>>> serviceUnavailable(ServerWebExchange exchange) {
        Throwable cause = exchange.getAttribute(
                ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 503);
        body.put("error", "Service Unavailable");
        body.put("message", "O serviço está temporariamente indisponível. Por favor, tente mais tarde.");
        if (cause != null) {
            body.put("cause", cause.getMessage());
        }

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body));
    }
}
