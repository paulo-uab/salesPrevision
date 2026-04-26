package com.uab.core.dto.template;

import com.uab.core.enums.FileType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class TemplateResponse {
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
    private List<TemplateFieldResponse> fields;
}
