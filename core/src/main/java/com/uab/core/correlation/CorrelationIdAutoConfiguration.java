package com.uab.core.correlation;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CorrelationIdAutoConfiguration {

    @Bean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    public RestClientCustomizer correlationIdRestClientCustomizer() {
        return builder -> builder.requestInterceptor(new CorrelationIdRestClientInterceptor());
    }
}
