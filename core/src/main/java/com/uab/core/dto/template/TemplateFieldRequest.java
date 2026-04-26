package com.uab.core.dto.template;

import com.uab.core.enums.FieldDataType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TemplateFieldRequest {

    @NotBlank
    private String fieldName;

    @NotBlank
    private String sourceName;

    @NotNull
    private FieldDataType dataType;

    private Boolean required = false;
    private Integer positionIndex;
    private String dateFormat;
    private String defaultValue;
    private String validationRegex;
    private String targetPath;
    private Boolean active = true;

    @Valid
    private List<TemplateValidationRuleRequest> rules = new ArrayList<>();
}
