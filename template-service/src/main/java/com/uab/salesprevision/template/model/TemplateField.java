package com.uab.salesprevision.template.model;

import com.uab.core.enums.FieldDataType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "template_fields")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private IngestionTemplate template;

    @Column(nullable = false, length = 120)
    private String fieldName;

    @Column(length = 120)
    private String sourceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FieldDataType dataType;

    private Boolean required = false;

    private Integer positionIndex;

    @Column(length = 50)
    private String dateFormat;

    @Column(length = 255)
    private String defaultValue;

    @Column(length = 500)
    private String validationRegex;

    @Column(length = 255)
    private String targetPath;

    private Boolean active = true;

    @Builder.Default
    @OneToMany(mappedBy = "field", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TemplateValidationRule> rules = new ArrayList<>();

}
