package com.uab.salesprevision.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long start = Instant.now().toEpochMilli();
        // CorrelationIdGatewayFilter runs first (HIGHEST_PRECEDENCE), so the header is
        // already present in the (mutated) request when LoggingFilter executes.
        String cid = request.getHeaders().getFirst(CorrelationIdGatewayFilter.HEADER);

        log.info("[{}] → {} {}", cid, request.getMethod(), request.getURI().getPath());

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long elapsed = Instant.now().toEpochMilli() - start;
            log.info("[{}] ← {} {} {} ({}ms)",
                    cid,
                    request.getMethod(),
                    request.getURI().getPath(),
                    exchange.getResponse().getStatusCode(),
                    elapsed);
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
