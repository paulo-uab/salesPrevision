package com.uab.salesprevision.template.dto;

import com.uab.core.enums.FieldDataType;
import com.uab.core.enums.FileType;
import com.uab.core.enums.TemplateRuleType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionTemplateResponse {

    private Long id;
    private String name;
    private String description;
    private FileType fileType;
    private String delimiter;
    private Boolean hasHeader;
    private String sheetName;
    private String rootPath;
    private Integer version;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<FieldResponse> fields = new ArrayList<>();

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldResponse {
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

        @Builder.Default
        private List<RuleResponse> rules = new ArrayList<>();
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleResponse {
        private Long id;
        private TemplateRuleType ruleType;
        private String ruleValue;
        private String message;
        private Boolean active;
    }
}
