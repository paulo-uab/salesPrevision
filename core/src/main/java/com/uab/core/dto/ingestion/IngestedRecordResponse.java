package com.uab.core.dto.ingestion;


import com.uab.core.enums.ValidationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class IngestedRecordResponse {
    private Long id;
    private Long ingestionJobId;
    private Long recordIndex;
    private ValidationStatus validationStatus;
    private String rawPayload;
    private String normalizedPayload;
    private LocalDateTime createdAt;
}
