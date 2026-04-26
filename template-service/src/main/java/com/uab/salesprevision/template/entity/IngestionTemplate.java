package com.uab.salesprevision.template.entity;

import com.uab.core.enums.FileType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ingestion_templates")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FileType fileType;

    @Column(length = 10)
    private String delimiter;

    private Boolean hasHeader;

    @Column(length = 150)
    private String sheetName;

    @Column(length = 255)
    private String rootPath;

    private Integer version;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TemplateField> fields = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (version == null) version = 1;
        if (active == null) active = true;
        if (hasHeader == null) hasHeader = true;
        if (fileType == null) fileType = FileType.UNKNOWN;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
