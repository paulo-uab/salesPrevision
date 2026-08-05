package com.uab.salesprevision.template.model;

import com.uab.core.enums.FileType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ingestion_templates",
        uniqueConstraints = @UniqueConstraint(columnNames = {"company_id", "name"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FileType fileType = FileType.UNKNOWN;

    @Column(length = 10)
    private String delimiter;

    @Builder.Default
    private Boolean hasHeader = true;

    @Column(length = 150)
    private String sheetName;

    @Column(length = 255)
    private String rootPath;

    @Builder.Default
    private Integer version = 1;

    @Builder.Default
    private Boolean active = true;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TemplateField> fields = new ArrayList<>();

}
