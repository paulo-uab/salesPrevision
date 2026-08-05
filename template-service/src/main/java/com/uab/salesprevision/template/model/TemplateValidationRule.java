package com.uab.salesprevision.template.model;

import com.uab.core.enums.TemplateRuleType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "template_validation_rules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateValidationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "field_id", nullable = false)
    private TemplateField field;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TemplateRuleType ruleType;

    @Column(length = 500)
    private String ruleValue;

    @Column(length = 500)
    private String message;

    private Boolean active = true;

}
