package com.uab.salesprevision.batch.config;

import com.uab.core.serviceauth.ServiceTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Deliberately NOT wired through core's ServiceAuthAutoConfiguration (which would
 * customize every RestClient.Builder in this service, including
 * ForecastItemWriter's client for the arbitrary, user-configured prediction API
 * URL — leaking this internal token to a third party). Instead IngestionClient
 * and PipelineClient take this bean directly and apply the interceptor only to
 * their own RestClient.
 */
@Configuration
public class InternalServiceAuthConfig {

    @Bean
    public ServiceTokenProvider serviceTokenProvider(
            @Value("${internal.service.auth-url}") String authUrl,
            @Value("${internal.service.username}") String username,
            @Value("${internal.service.password}") String password) {
        return new ServiceTokenProvider(authUrl, username, password);
    }
}
