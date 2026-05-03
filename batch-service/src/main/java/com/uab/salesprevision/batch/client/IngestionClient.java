package com.uab.salesprevision.batch.client;

import com.uab.core.dto.ingestion.IngestedRecordResponse;
import com.uab.core.dto.ingestion.IngestionJobResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class IngestionClient {

    private final RestClient restClient;
    private final String jobsUri;

    public IngestionClient(
            @Value("${services.ingestion.url}") String baseUrl,
            @Value("${services.ingestion.uri}") String jobsUri,
            RestClient.Builder builder) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.jobsUri = jobsUri;
    }

    public List<IngestionJobResponse> getCompletedJobs(Long templateId) {
        return restClient.get()
                .uri(jobsUri + "?templateId={templateId}&status=COMPLETED", templateId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<IngestionJobResponse>>() {});
    }

    public Map<String, Object> getRecordsPage(Long jobId, int page, int size) {
        return restClient.get()
                .uri(jobsUri + "/{jobId}/records?page={page}&size={size}", jobId, page, size)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
    }
}
