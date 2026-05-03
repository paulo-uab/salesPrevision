package com.uab.salesprevision.batch.client;

import com.uab.core.dto.pipeline.PipelineDto;
import com.uab.core.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PipelineClient {

    private final RestClient restClient;
    private final String pipelineUri;

    public PipelineClient(
            @Value("${services.pipeline.url}") String baseUrl,
            @Value("${services.pipeline.uri}") String pipelineUri,
            RestClient.Builder builder) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.pipelineUri = pipelineUri;
    }

    public PipelineDto getPipeline(Long pipelineId) {
        return restClient.get()
                .uri(pipelineUri + pipelineId + "/dto")
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new ResourceNotFoundException("error.pipeline.not.found", pipelineId);
                })
                .body(PipelineDto.class);
    }
}
