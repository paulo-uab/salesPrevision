package com.uab.salesprevision.pipeline.client.dto;

import com.uab.core.enums.FieldDataType;
import com.uab.core.enums.FileType;
import com.uab.core.enums.TemplateRuleType;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TemplateClientDto {

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

    private List<FieldDto> fields = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldDto {
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

        private List<RuleDto> rules = new ArrayList<>();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleDto {
        private Long id;
        private TemplateRuleType ruleType;
        private String ruleValue;
        private String message;
        private Boolean active;
    }
}
