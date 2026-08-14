package com.uab.salesprevision.template.dto;

import com.uab.core.enums.FileType;
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
@Schema(description = "Replaces a template's definition, including its fields — same shape as the create request. Bumps the template's version.")
public class UpdateIngestionTemplateRequest {

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

    @Schema(description = "Fields expected in the file — replaces the existing field list entirely.")
    @Valid
    @Builder.Default
    private List<CreateIngestionTemplateRequest.FieldRequest> fields = new ArrayList<>();
}
