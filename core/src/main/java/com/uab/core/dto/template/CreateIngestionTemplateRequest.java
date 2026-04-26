package com.uab.core.dto.template;

import com.uab.core.enums.FieldDataType;
import com.uab.core.enums.FileType;
import com.uab.core.enums.TemplateRuleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateIngestionTemplateRequest {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private FileType fileType;

    private String delimiter;
    private Boolean hasHeader;
    private String sheetName;
    private String rootPath;
    private Integer version;
    private Boolean active;

    @Valid
    @Builder.Default
    private List<FieldRequest> fields = new ArrayList<>();

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldRequest {

        @NotBlank
        private String fieldName;

        private String sourceName;

        @NotNull
        private FieldDataType dataType;

        private Boolean required;
        private Integer positionIndex;
        private String dateFormat;
        private String defaultValue;
        private String validationRegex;
        private String targetPath;
        private Boolean active;

        @Valid
        @Builder.Default
        private List<RuleRequest> rules = new ArrayList<>();
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleRequest {

        @NotNull
        private TemplateRuleType ruleType;

        private String ruleValue;
        private String message;
        private Boolean active;
    }
}
