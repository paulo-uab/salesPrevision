package com.uab.salesprevision.batch.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "batch_schedule_configs")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class BatchScheduleConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private Long pipelineId;

    @Column(nullable = false, length = 150)
    private String pipelineName;

    @Column(nullable = false, length = 150)
    private String cronExpression;

    @Column(nullable = false)
    private Integer lookbackDays = 7;

    @Column(nullable = false, length = 500)
    private String predictionApiUrl;

    @Column(nullable = false)
    private Boolean active = true;

    // Only set true when predictionApiUrl points at our own prediction-service —
    // ForecastItemWriter attaches the internal-service token when this is true.
    // Must stay opt-in: predictionApiUrl is otherwise an arbitrary user-configured
    // URL, and the token must never be sent to a third party by default.
    @Column(nullable = false)
    private Boolean internalPrediction = false;

    @Column(length = 100)
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Written by BatchScheduler right before each trigger, so the cron poller's
    // notion of "last run" survives a restart instead of resetting to a fresh,
    // empty in-memory map (which would otherwise let a schedule fire again
    // immediately after every deploy/restart).
    private LocalDateTime lastRunAt;

    @OneToMany(mappedBy = "scheduleConfig", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BatchExecution> executions = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.active == null) this.active = true;
        if (this.lookbackDays == null) this.lookbackDays = 7;
        if (this.internalPrediction == null) this.internalPrediction = false;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
