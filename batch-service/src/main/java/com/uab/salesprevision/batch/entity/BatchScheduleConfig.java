package com.uab.salesprevision.batch.entity;

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

    @Column(length = 100)
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "scheduleConfig", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BatchExecution> executions = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.active == null) this.active = true;
        if (this.lookbackDays == null) this.lookbackDays = 7;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
