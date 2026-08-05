package com.uab.salesprevision.batch.model;

import com.uab.core.enums.BatchExecutionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "batch_executions")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class BatchExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_config_id", nullable = false)
    private BatchScheduleConfig scheduleConfig;

    private Long springBatchJobExecutionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BatchExecutionStatus status;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    @Column(nullable = false)
    private Long recordsSent = 0L;

    @Column(nullable = false)
    private Long recordsFailed = 0L;

    @Column(length = 4000)
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.recordsSent == null) this.recordsSent = 0L;
        if (this.recordsFailed == null) this.recordsFailed = 0L;
    }
}
