package com.uab.salesprevision.template.dto;

import com.uab.core.enums.FileType;
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
public class UpdateIngestionTemplateRequest {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private FileType fileType;

    private String delimiter;
    private Boolean hasHeader;
    private String sheetName;
    private String rootPath;

    @Valid
    @Builder.Default
    private List<CreateIngestionTemplateRequest.FieldRequest> fields = new ArrayList<>();
}
