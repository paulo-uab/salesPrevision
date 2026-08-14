package com.uab.salesprevision.ingestion.client;

import com.uab.salesprevision.ingestion.client.dto.TemplateClientDto;
import com.uab.core.exception.ResourceNotFoundException;
import com.uab.core.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class TemplateClient {

    private final RestClient restClient;

    @Value("${services.template.uri}")
    private String serviceUri;

    public TemplateClient(RestClient.Builder builder,
                          @Value("${services.template.url}") String templateServiceUrl) {
        this.restClient = builder.baseUrl(templateServiceUrl).build();
    }

    // A 404 (ResourceNotFoundException) is ignored by both annotations (see
    // application.properties) — it's a legitimate business outcome, not a
    // transient failure, so it should never be retried or count against the
    // circuit breaker. Only connectivity/5xx failures trigger retry → fallback.
    @Retry(name = "template-service")
    @CircuitBreaker(name = "template-service", fallbackMethod = "templateServiceUnavailable")
    public TemplateClientDto getTemplate(Long templateId) {
        try {
            return restClient.get()
                    .uri(serviceUri + "{id}", templateId)
                    .retrieve()
                    .body(TemplateClientDto.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("error.template.not.found", templateId);
        }
    }

    private TemplateClientDto templateServiceUnavailable(Long templateId, Throwable ex) {
        log.error("template-service unavailable after retries: templateId={}", templateId, ex);
        throw new ServiceUnavailableException("error.template.service.unavailable", templateId);
    }
}
