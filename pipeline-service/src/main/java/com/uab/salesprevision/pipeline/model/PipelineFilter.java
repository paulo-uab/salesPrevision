package com.uab.salesprevision.pipeline.model;

import com.uab.core.enums.FilterOperator;
import com.uab.core.enums.LogicalOperator;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pipeline_filters")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class PipelineFilter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_id", nullable = false)
    private ForecastPipeline pipeline;

    @Column(nullable = false, length = 120)
    private String fieldName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FilterOperator operator;

    @Column(length = 500)
    private String value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LogicalOperator logicalOperator = LogicalOperator.AND;

    private Integer orderIndex;
}
