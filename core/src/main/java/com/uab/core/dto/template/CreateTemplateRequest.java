package com.uab.core.dto.template;

import com.uab.core.enums.FileType;
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
public class CreateTemplateRequest {

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private FileType fileType;

    private String delimiter;
    private Boolean hasHeader = true;
    private String sheetName;
    private String rootPath;
    private Integer version = 1;
    private Boolean active = true;

    @Valid
    private List<TemplateFieldRequest> fields = new ArrayList<>();
}
