package com.uab.salesprevision.ingestion.entity;

import com.uab.core.enums.ValidationStatus;
import jakarta.persistence.*;
import lombok.*;

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
    private ValidationStatus validationStatus;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.validationStatus == null) this.validationStatus = ValidationStatus.VALID;
    }
}
