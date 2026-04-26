package com.uab.core.dto.template;

import com.uab.core.enums.FieldDataType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class TemplateFieldResponse {
    private Long id;
    private String fieldName;
    private String sourceName;
    private FieldDataType dataType;
    private Boolean required;
    private Integer positionIndex;
    private String dateFormat;
    private String defaultValue;
    private String validationRegex;
    private String targetPath;
    private Boolean active;
    private List<TemplateValidationRuleResponse> rules;
}
