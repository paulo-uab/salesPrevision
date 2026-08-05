package com.uab.salesprevision.ingestion.kafka;

import com.uab.core.correlation.CorrelationId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IngestionEventProducer {

    public static final String TOPIC = "ingestion.jobs";

    private final KafkaTemplate<String, IngestionJobEvent> kafkaTemplate;

    public void publish(Long jobId) {
        String correlationId = MDC.get(CorrelationId.MDC_KEY);
        kafkaTemplate.send(TOPIC, String.valueOf(jobId), new IngestionJobEvent(jobId, correlationId));
        log.info("[{}] Event published to topic '{}': jobId={}", correlationId, TOPIC, jobId);
    }
}
