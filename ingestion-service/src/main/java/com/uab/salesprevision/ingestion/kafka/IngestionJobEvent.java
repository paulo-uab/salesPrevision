package com.uab.salesprevision.ingestion.kafka;

public record IngestionJobEvent(Long jobId, String correlationId) {}
