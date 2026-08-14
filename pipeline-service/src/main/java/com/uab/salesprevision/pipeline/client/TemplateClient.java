package com.uab.salesprevision.pipeline.client;

import com.uab.salesprevision.pipeline.client.dto.TemplateClientDto;
import com.uab.core.exception.ResourceNotFoundException;
import com.uab.core.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class TemplateClient {

    private final RestClient restClient;
    private final String templateUri;

    public TemplateClient(
            @Value("${services.template.url}") String baseUrl,
            @Value("${services.template.uri}") String templateUri,
            RestClient.Builder builder) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.templateUri = templateUri;
    }

    // A 404 (ResourceNotFoundException) is ignored by both annotations (see
    // application.properties) — it's a legitimate business outcome, not a
    // transient failure, so it should never be retried or count against the
    // circuit breaker. Only connectivity/5xx failures trigger retry → fallback.
    @Retry(name = "template-service")
    @CircuitBreaker(name = "template-service", fallbackMethod = "templateServiceUnavailable")
    public TemplateClientDto getTemplate(Long templateId) {
        log.debug("Fetching template id={} from template-service", templateId);
        TemplateClientDto template = restClient.get()
                .uri(templateUri + templateId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    log.warn("Template not found: id={}", templateId);
                    throw new ResourceNotFoundException("error.template.not.found", templateId);
                })
                .body(TemplateClientDto.class);
        log.debug("Template fetched: id={}, name='{}'", templateId, template != null ? template.getName() : null);
        return template;
    }

    private TemplateClientDto templateServiceUnavailable(Long templateId, Throwable ex) {
        log.error("template-service unavailable after retries: templateId={}", templateId, ex);
        throw new ServiceUnavailableException("error.template.service.unavailable", templateId);
    }
}
