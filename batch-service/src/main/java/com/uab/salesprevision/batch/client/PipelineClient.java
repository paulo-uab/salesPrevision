package com.uab.salesprevision.batch.client;

import com.uab.core.serviceauth.ServiceAuthRestClientInterceptor;
import com.uab.core.serviceauth.ServiceTokenProvider;
import com.uab.salesprevision.batch.client.dto.PipelineClientDto;
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
public class PipelineClient {

    private final RestClient restClient;
    private final String pipelineUri;

    public PipelineClient(
            @Value("${services.pipeline.url}") String baseUrl,
            @Value("${services.pipeline.uri}") String pipelineUri,
            RestClient.Builder builder,
            ServiceTokenProvider serviceTokenProvider) {
        this.restClient = builder.baseUrl(baseUrl)
                .requestInterceptor(new ServiceAuthRestClientInterceptor(serviceTokenProvider))
                .build();
        this.pipelineUri = pipelineUri;
    }

    // A 404 (ResourceNotFoundException) is ignored by both annotations (see
    // application.properties) — it's a legitimate business outcome, not a
    // transient failure, so it should never be retried or count against the
    // circuit breaker. Only connectivity/5xx failures trigger retry → fallback.
    @Retry(name = "pipeline-service")
    @CircuitBreaker(name = "pipeline-service", fallbackMethod = "pipelineServiceUnavailable")
    public PipelineClientDto getPipeline(Long pipelineId, Long companyId) {
        log.debug("Fetching pipeline id={} from pipeline-service, companyId={}", pipelineId, companyId);
        PipelineClientDto pipeline = restClient.get()
                .uri(pipelineUri + pipelineId + "/dto")
                .header("X-Company-Id", String.valueOf(companyId))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    log.warn("Pipeline not found: id={}", pipelineId);
                    throw new ResourceNotFoundException("error.pipeline.not.found", pipelineId);
                })
                .body(PipelineClientDto.class);
        log.debug("Pipeline fetched: id={}, name='{}'", pipelineId, pipeline != null ? pipeline.getName() : null);
        return pipeline;
    }

    private PipelineClientDto pipelineServiceUnavailable(Long pipelineId, Long companyId, Throwable ex) {
        log.error("pipeline-service unavailable after retries: pipelineId={}", pipelineId, ex);
        throw new ServiceUnavailableException("error.pipeline.service.unavailable", pipelineId);
    }
}
