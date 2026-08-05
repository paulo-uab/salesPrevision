package com.uab.core.tokenrelay;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * Activated only in services that opt in with {@code services.auth.relay=true} —
 * attaches the inbound request's Authorization header to every outbound
 * RestClient call. Do not enable this in a service whose RestClient beans are
 * also used to call arbitrary external/user-configured URLs (e.g.
 * batch-service's prediction API client), since that would leak the caller's
 * token to third parties.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "services.auth", name = "relay", havingValue = "true")
public class TokenRelayAutoConfiguration {

    @Bean
    public RestClientCustomizer tokenRelayRestClientCustomizer() {
        return builder -> builder.requestInterceptor(new TokenRelayRestClientInterceptor());
    }
}
