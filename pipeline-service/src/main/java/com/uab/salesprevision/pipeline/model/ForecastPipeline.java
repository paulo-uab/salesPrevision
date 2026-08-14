package com.uab.salesprevision.pipeline.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "forecast_pipelines",
        uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "name"}))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ForecastPipeline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private Long templateId;

    @Column(nullable = false, length = 150)
    private String templateName;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    // Modelo usado para a previsão "real" deste pipeline. Valores têm de bater
    // certo com o ModelType (Literal) do prediction-service — validado lá, não aqui.
    @Builder.Default
    @Column(nullable = false, length = 30)
    private String forecastModel = "naive";

    // Modelo de controlo: corre sempre em paralelo ao forecastModel sobre os
    // mesmos dados, para haver sempre uma comparação baseline vs. modelo escolhido
    // (é o que o professor pediu para a avaliação experimental).
    @Builder.Default
    @Column(nullable = false, length = 30)
    private String controlModel = "naive";

    @Builder.Default
    @Column(nullable = false, length = 10)
    private String frequency = "ME";

    @Builder.Default
    @Column(nullable = false)
    private Integer forecastHorizon = 12;

    private Integer seasonPeriod;

    // CSV de 3 inteiros, ex. "1,1,1" — representação simples e sem ambiguidade
    // ao atravessar para o Python, que faz o parse para list[int].
    @Column(length = 20)
    private String arimaOrder;

    @Builder.Default
    @Column(nullable = false)
    private Integer nLags = 12;

    @Builder.Default
    @Column(nullable = false)
    private Boolean includeDateFeatures = true;

    // Só tem efeito real para modelos com suporte a atualização incremental
    // (arima, xgboost hoje) — o prediction-service ignora-a para os restantes.
    @Builder.Default
    @Column(nullable = false)
    private Boolean incrementalTraining = false;

    @OneToMany(mappedBy = "pipeline", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PipelineField> fields = new ArrayList<>();

    @OneToMany(mappedBy = "pipeline", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PipelineFilter> filters = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
