package com.uab.salesprevision.template.mappers;

import com.uab.salesprevision.template.dto.CreateIngestionTemplateRequest;
import com.uab.salesprevision.template.dto.IngestionTemplateResponse;
import com.uab.salesprevision.template.model.IngestionTemplate;
import com.uab.salesprevision.template.model.TemplateField;
import com.uab.salesprevision.template.model.TemplateValidationRule;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface TemplateEntitiesMapper {

    // Request → Entity

    @Mapping(target = "name", expression = "java(request.getName().trim())")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    IngestionTemplate createRequestToIngestionTemplate(CreateIngestionTemplateRequest request);

    @Mapping(target = "fieldName", expression = "java(fieldRequest.getFieldName().trim())")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "template", ignore = true)
    TemplateField fieldRequestToTemplateField(CreateIngestionTemplateRequest.FieldRequest fieldRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "field", ignore = true)
    TemplateValidationRule ruleRequestToTemplateValidationRule(CreateIngestionTemplateRequest.RuleRequest ruleRequest);

    @AfterMapping
    default void setBackReferences(@MappingTarget IngestionTemplate template) {
        if (template.getFields() == null) return;
        for (TemplateField field : template.getFields()) {
            field.setTemplate(template);
            if (field.getRules() == null) continue;
            for (TemplateValidationRule rule : field.getRules()) {
                rule.setField(field);
            }
        }
    }

    // Entity → Response

    IngestionTemplateResponse ingestionTemplateToIngestionTemplateResponse(IngestionTemplate template);

    IngestionTemplateResponse.FieldResponse templateFieldToFieldResponse(TemplateField field);

    IngestionTemplateResponse.RuleResponse templateValidationRuleToRuleResponse(TemplateValidationRule rule);
}
