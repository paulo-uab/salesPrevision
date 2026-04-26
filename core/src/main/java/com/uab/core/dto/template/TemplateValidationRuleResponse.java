package com.uab.core.dto.template;

import com.uab.core.enums.TemplateRuleType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TemplateValidationRuleResponse {
    private Long id;
    private TemplateRuleType ruleType;
    private String ruleValue;
    private String message;
    private Boolean active;
}
