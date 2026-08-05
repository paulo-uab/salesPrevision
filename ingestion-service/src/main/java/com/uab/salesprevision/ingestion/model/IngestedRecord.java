package com.uab.salesprevision.ingestion.model;

import com.uab.core.enums.ValidationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "ingested_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestedRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingestion_job_id", nullable = false)
    private IngestionJob ingestionJob;

    @Column(nullable = false)
    private Long recordIndex;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String rawPayload;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String normalizedPayload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ValidationStatus validationStatus = ValidationStatus.VALID;

    @CreatedDate
    private LocalDateTime createdAt;

}
