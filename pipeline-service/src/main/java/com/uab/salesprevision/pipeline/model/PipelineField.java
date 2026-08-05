package com.uab.salesprevision.pipeline.model;

import com.uab.core.enums.TransformationType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pipeline_fields")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class PipelineField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_id", nullable = false)
    private ForecastPipeline pipeline;

    @Column(nullable = false, length = 120)
    private String sourceFieldName;

    @Column(nullable = false, length = 120)
    private String targetFieldName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransformationType transformationType = TransformationType.NONE;

    @Column(columnDefinition = "TEXT")
    private String transformationConfig;

    private Integer positionIndex;

    @Column(nullable = false)
    private Boolean active = true;
}
