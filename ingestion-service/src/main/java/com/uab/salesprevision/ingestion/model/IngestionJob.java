package com.uab.salesprevision.ingestion.model;

import com.uab.core.enums.IngestionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

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

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private Long templateId;

    @Column(nullable = false, length = 150)
    private String templateName;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IngestionStatus status = IngestionStatus.RECEIVED;

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

    @CreatedDate
    private LocalDateTime createdAt;

    @Column(length = 100)
    private String createdBy;

    @Column(length = 4000)
    private String errorMessage;

    @Builder.Default
    @Column(nullable = false)
    private Long recordCount = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Long errorCount = 0L;

}
