package com.uab.salesprevision.template.dto;

import com.uab.core.enums.FieldDataType;
import com.uab.core.enums.FileType;
import com.uab.core.enums.TemplateRuleType;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Defines the expected shape of a file that can be ingested: its format, and the fields it contains with their types and validation rules.")
public class CreateIngestionTemplateRequest {

    @Schema(description = "Template name, unique within the company.", example = "Retail Sales Dataset")
    @NotBlank(message = "{validation.template.name.required}")
    private String name;

    @Schema(description = "Free-text description of what this template represents.")
    private String description;

    @Schema(description = "Format of the files this template expects.", example = "CSV")
    @NotNull(message = "{validation.template.file.type.required}")
    private FileType fileType;

    @Schema(description = "CSV column delimiter. Use \"\\t\" for tab; any other value is used literally. Ignored for non-CSV file types.", example = ",")
    private String delimiter;

    @Schema(description = "Whether CSV files matched to this template have a header row.")
    private Boolean hasHeader;

    @Schema(description = "Sheet name to read, for XLSX files only.")
    private String sheetName;

    @Schema(description = "JSON root path to the array of records, for JSON files only.")
    private String rootPath;

    @Schema(description = "Fields expected in the file, in the order they should be validated.")
    @Valid
    @Builder.Default
    private List<FieldRequest> fields = new ArrayList<>();

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "A single expected field: its canonical name, source column/key, data type, and validation rules.")
    public static class FieldRequest {

        @Schema(description = "Canonical field name used internally by the system.", example = "total_amount")
        @NotBlank(message = "{validation.field.name.required}")
        private String fieldName;

        @Schema(description = "Column header (CSV) or key (JSON) in the source file. Falls back to positionIndex when the file has no header.", example = "Total Amount")
        private String sourceName;

        @Schema(description = "Type the raw string value is converted to after validation.", example = "DECIMAL")
        @NotNull(message = "{validation.field.data.type.required}")
        private FieldDataType dataType;

        @Schema(description = "Whether this field must be present and non-blank in every row.")
        private Boolean required;

        @Schema(description = "0-based column position, used as a fallback when the file has no header row.")
        private Integer positionIndex;

        @Schema(description = "Expected date/time pattern, only relevant for DATE/DATETIME fields (e.g. \"yyyy-MM-dd\").")
        private String dateFormat;

        @Schema(description = "Value used when the source value is missing.")
        private String defaultValue;

        @Schema(description = "Regular expression the raw value must match, applied before type conversion.")
        private String validationRegex;

        @Schema(description = "Destination path in the normalized payload, for nested/structured targets.")
        private String targetPath;

        @Schema(description = "Validation rules applied to this field, in addition to required/dataType.")
        @Valid
        @Builder.Default
        private List<RuleRequest> rules = new ArrayList<>();
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "A single validation rule attached to a field.")
    public static class RuleRequest {

        @Schema(description = "Kind of validation to apply.", example = "MIN")
        @NotNull(message = "{validation.rule.type.required}")
        private TemplateRuleType ruleType;

        @Schema(description = "Rule parameter — meaning depends on ruleType (e.g. the regex pattern for REGEX, the minimum value for MIN, a comma-separated list for ENUM).", example = "0")
        private String ruleValue;

        @Schema(description = "Custom error message shown when this rule fails. Falls back to a default Portuguese message when omitted.")
        private String message;
    }
}
