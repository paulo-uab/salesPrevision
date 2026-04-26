package com.uab.salesprevision.ingestion.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ingestion_errors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngestionError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingestion_job_id", nullable = false)
    private IngestionJob ingestionJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id")
    private IngestedRecord record;

    @Column(length = 100)
    private String fieldName;

    @Column(nullable = false, length = 100)
    private String errorType;

    @Column(nullable = false, length = 4000)
    private String message;

    @Column(length = 1000)
    private String rawValue;

    private Long lineNumber;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
