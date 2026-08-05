package com.uab.salesprevision.pipeline.client;

import com.uab.salesprevision.pipeline.client.dto.TemplateClientDto;
import com.uab.core.exception.ResourceNotFoundException;
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
}
