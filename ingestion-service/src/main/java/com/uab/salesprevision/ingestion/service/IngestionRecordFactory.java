package com.uab.salesprevision.ingestion.service;


import com.uab.salesprevision.ingestion.model.IngestedRecord;
import com.uab.salesprevision.ingestion.model.IngestionError;
import com.uab.salesprevision.ingestion.model.IngestionJob;
import com.uab.core.enums.ValidationStatus;
import org.springframework.stereotype.Service;

@Service
public class IngestionRecordFactory {

    public IngestedRecord buildRecord(IngestionJob job,
                                      long recordIndex,
                                      String rawPayload,
                                      String normalizedPayload,
                                      ValidationStatus validationStatus) {
        return IngestedRecord.builder()
                .ingestionJob(job)
                .recordIndex(recordIndex)
                .rawPayload(rawPayload)
                .normalizedPayload(normalizedPayload)
                .validationStatus(validationStatus)
                .build();
    }

    public IngestionError buildError(IngestionJob job,
                                     IngestedRecord record,
                                     String fieldName,
                                     String errorType,
                                     String message,
                                     String rawValue,
                                     Long lineNumber) {
        return IngestionError.builder()
                .ingestionJob(job)
                .record(record)
                .fieldName(fieldName)
                .errorType(errorType)
                .message(message)
                .rawValue(rawValue)
                .lineNumber(lineNumber)
                .build();
    }
}
