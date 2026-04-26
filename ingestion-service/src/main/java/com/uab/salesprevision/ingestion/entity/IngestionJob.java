package com.uab.salesprevision.ingestion.entity;

import com.uab.core.enums.IngestionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ingestion_jobs")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class IngestionJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long templateId;

    @Column(nullable = false, length = 150)
    private String templateName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IngestionStatus status;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Column(nullable = false, length = 255)
    private String storedFileName;

    @Column(length = 100)
    private String contentType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false, length = 500)
    private String storagePath;

    @Column(length = 128)
    private String checksum;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 100)
    private String createdBy;

    @Column(length = 4000)
    private String errorMessage;

    @Column(nullable = false)
    private Long recordCount = 0L;

    @Column(nullable = false)
    private Long errorCount = 0L;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = IngestionStatus.RECEIVED;
        if (this.recordCount == null) this.recordCount = 0L;
        if (this.errorCount == null) this.errorCount = 0L;
    }
}
