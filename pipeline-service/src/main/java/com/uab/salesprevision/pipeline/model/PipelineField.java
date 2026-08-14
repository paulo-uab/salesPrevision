package com.uab.salesprevision.pipeline.model;

import com.uab.core.enums.ForecastFieldRole;
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

    // Marca o papel deste campo (já transformado, targetFieldName) na previsão —
    // é a partir daqui que o batch-service deriva date_field/target_fields/group_field
    // sem duplicar nomes de campo numa segunda configuração.
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ForecastFieldRole forecastRole = ForecastFieldRole.NONE;

    // Só relevante quando forecastRole=TARGET (sum/mean/last/max/min) — valores
    // têm de corresponder ao AggregationType do prediction-service (Python).
    @Column(length = 20)
    private String aggregation;
}
