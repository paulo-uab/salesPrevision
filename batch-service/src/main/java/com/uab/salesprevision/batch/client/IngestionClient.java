package com.uab.salesprevision.batch.client;

import com.uab.core.serviceauth.ServiceAuthRestClientInterceptor;
import com.uab.core.serviceauth.ServiceTokenProvider;
import com.uab.salesprevision.batch.client.dto.IngestionJobDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class IngestionClient {

    private final RestClient restClient;
    private final String jobsUri;

    public IngestionClient(
            @Value("${services.ingestion.url}") String baseUrl,
            @Value("${services.ingestion.uri}") String jobsUri,
            RestClient.Builder builder,
            ServiceTokenProvider serviceTokenProvider) {
        this.restClient = builder.baseUrl(baseUrl)
                .requestInterceptor(new ServiceAuthRestClientInterceptor(serviceTokenProvider))
                .build();
        this.jobsUri = jobsUri;
    }

    public List<IngestionJobDto> getCompletedJobs(Long templateId, Long companyId) {
        log.debug("Fetching completed jobs: templateId={}, companyId={}", templateId, companyId);
        List<IngestionJobDto> jobs = restClient.get()
                .uri(jobsUri + "?templateId={templateId}&status=COMPLETED", templateId)
                .header("X-Company-Id", String.valueOf(companyId))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        log.debug("Fetched {} completed jobs for templateId={}", jobs != null ? jobs.size() : 0, templateId);
        return jobs;
    }

    public Map<String, Object> getRecordsPage(Long jobId, int page, int size, Long companyId) {
        log.debug("Fetching records page: jobId={}, page={}, size={}, companyId={}", jobId, page, size, companyId);
        return restClient.get()
                .uri(jobsUri + "/{jobId}/records?page={page}&size={size}", jobId, page, size)
                .header("X-Company-Id", String.valueOf(companyId))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
