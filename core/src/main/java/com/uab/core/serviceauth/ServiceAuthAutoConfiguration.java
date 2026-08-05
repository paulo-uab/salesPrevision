package com.uab.core.serviceauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * Activated only in services that define {@code services.gateway.url} — attaches
 * a bearer token (obtained from the gateway) to every outbound RestClient call.
 * Only wire this into services whose RestClient beans are exclusively used to
 * call other internal services through the gateway: it must never be enabled in
 * a service that also calls arbitrary external/user-configured URLs (e.g.
 * batch-service's prediction API client), since that would leak the internal
 * service token to third parties.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "services.gateway", name = "url")
public class ServiceAuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ServiceTokenProvider serviceTokenProvider(
            @Value("${services.gateway.url}") String gatewayUrl,
            @Value("${services.gateway.auth.username}") String username,
            @Value("${services.gateway.auth.password}") String password) {
        return new ServiceTokenProvider(gatewayUrl, username, password);
    }

    @Bean
    public RestClientCustomizer serviceAuthRestClientCustomizer(ServiceTokenProvider tokenProvider) {
        return builder -> builder.requestInterceptor(new ServiceAuthRestClientInterceptor(tokenProvider));
    }
}
