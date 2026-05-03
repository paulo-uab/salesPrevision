package com.uab.salesprevision.ingestion.client;

import com.uab.core.dto.ingestion.TemplateDto;
import com.uab.core.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class TemplateClient {

    private final RestClient restClient;

    @Value("${services.template.uri}")
    private String serviceUri;

    public TemplateClient(RestClient.Builder builder,
                          @Value("${services.template.url}") String templateServiceUrl) {
        this.restClient = builder.baseUrl(templateServiceUrl).build();
    }

    public TemplateDto getTemplate(Long templateId) {
        try {
            return restClient.get()
                    .uri(serviceUri + "{id}", templateId)
                    .retrieve()
                    .body(TemplateDto.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("error.template.not.found", templateId);
        }
    }
}
