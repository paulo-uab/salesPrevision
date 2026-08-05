package com.uab.salesprevision.ingestion.kafka;

import com.uab.core.correlation.CorrelationId;
import com.uab.salesprevision.ingestion.service.IngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class IngestionEventConsumer {

    private final IngestionService ingestionService;
    private final MessageSource messageSource;

    @RetryableTopic(
            attempts = "3",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltTopicSuffix = ".dlt"
    )
    @KafkaListener(topics = IngestionEventProducer.TOPIC, groupId = "ingestion-processor")
    public void consume(IngestionJobEvent event) {
        setCorrelationId(event.correlationId());
        try {
            log.info("Received event: jobId={}", event.jobId());
            ingestionService.processJob(event.jobId());
        } finally {
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }

    @DltHandler
    public void onDeadLetter(IngestionJobEvent event) {
        setCorrelationId(event.correlationId());
        try {
            log.error("Job {} reached DLT after all retries. Marking as FAILED.", event.jobId());
            String errorMessage = messageSource.getMessage(
                    "error.ingestion.kafka.dlt.failed", null, Locale.forLanguageTag("pt"));
            ingestionService.markJobFailed(event.jobId(), errorMessage);
        } finally {
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }

    private void setCorrelationId(String correlationId) {
        if (correlationId != null) {
            MDC.put(CorrelationId.MDC_KEY, correlationId);
        }
    }
}
