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

import java.util.UUID;

@Component
public class CorrelationIdGatewayFilter implements GlobalFilter, Ordered {

    static final String HEADER = "X-Correlation-ID";

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdGatewayFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            log.debug("Correlation ID gerado: {}", correlationId);
        }

        final String cid = correlationId;

        // Propagate to downstream service via request header
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(HEADER, cid)
                .build();

        // Echo back on the response
        exchange.getResponse().beforeCommit(() -> {
            exchange.getResponse().getHeaders().set(HEADER, cid);
            return Mono.empty();
        });

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .contextWrite(ctx -> ctx.put(HEADER, cid));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
