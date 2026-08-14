package com.uab.salesprevision.template.service;

import com.uab.core.enums.FieldDataType;
import com.uab.core.enums.FileType;
import com.uab.salesprevision.template.dto.CreateIngestionTemplateRequest;
import com.uab.salesprevision.template.mappers.TemplateEntitiesMapperImpl;
import com.uab.salesprevision.template.model.IngestionTemplate;
import com.uab.salesprevision.template.model.TemplateField;
import com.uab.salesprevision.template.repository.IngestionTemplateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the REAL mapper (not mocked) through a REAL JPA save, to catch
 * exactly the kind of parent/child FK wiring bug that a mocked-repository
 * unit test can't see.
 */
@DataJpaTest
class CreateFlowIntegrationTest {

    @Autowired
    private IngestionTemplateRepository repository;

    private final TemplateEntitiesMapperImpl mapper = new TemplateEntitiesMapperImpl();

    @Test
    void mappedTemplateWithFields_savesWithCorrectForeignKey() {
        CreateIngestionTemplateRequest request = CreateIngestionTemplateRequest.builder()
                .name("Diagnostic Test Template")
                .fileType(FileType.CSV)
                .hasHeader(true)
                .fields(List.of(CreateIngestionTemplateRequest.FieldRequest.builder()
                        .fieldName("transactionId")
                        .sourceName("Transaction ID")
                        .dataType(FieldDataType.INTEGER)
                        .required(true)
                        .build()))
                .build();

        IngestionTemplate template = mapper.createRequestToIngestionTemplate(request);
        template.setCompanyId(1L);

        assertThat(template.getFields()).hasSize(1);
        TemplateField mappedField = template.getFields().get(0);
        assertThat(mappedField.getTemplate())
                .as("mapper's @AfterMapping should have set field.template back-reference before save")
                .isSameAs(template);

        IngestionTemplate saved = repository.saveAndFlush(template);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getFields()).hasSize(1);
        assertThat(saved.getFields().get(0).getTemplate().getId()).isEqualTo(saved.getId());
    }

    @Test
    void mappedTemplate_getsSensibleDefaults_whenRequestOmitsThem() {
        CreateIngestionTemplateRequest request = CreateIngestionTemplateRequest.builder()
                .name("Defaults Template")
                .fileType(FileType.CSV)
                .hasHeader(true)
                .fields(List.of(CreateIngestionTemplateRequest.FieldRequest.builder()
                        .fieldName("amount")
                        .dataType(FieldDataType.DECIMAL)
                        // required deliberately omitted
                        .build()))
                .build();

        IngestionTemplate template = mapper.createRequestToIngestionTemplate(request);
        template.setCompanyId(1L);

        IngestionTemplate saved = repository.saveAndFlush(template);

        assertThat(saved.getActive()).as("active should default to true, not stay null").isTrue();
        assertThat(saved.getVersion()).as("version should default to 1, not stay null").isEqualTo(1);
        assertThat(saved.getFields().get(0).getRequired())
                .as("required should default to false, not stay null").isFalse();
    }
}
