package com.uab.salesprevision.pipeline.client;

import com.uab.core.dto.ingestion.TemplateDto;
import com.uab.core.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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

    public TemplateDto getTemplate(Long templateId) {
        return restClient.get()
                .uri(templateUri + templateId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException("error.template.not.found", templateId);
                })
                .body(TemplateDto.class);
    }
}
