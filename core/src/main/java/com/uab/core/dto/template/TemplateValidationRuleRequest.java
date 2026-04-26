package com.uab.core.dto.template;


import com.uab.core.enums.TemplateRuleType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TemplateValidationRuleRequest {

    @NotNull
    private TemplateRuleType ruleType;

    private String ruleValue;
    private String message;
    private Boolean active = true;
}
