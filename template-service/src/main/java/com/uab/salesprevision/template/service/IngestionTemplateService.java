package com.uab.salesprevision.template.service;


import com.uab.core.dto.template.CreateIngestionTemplateRequest;
import com.uab.core.dto.template.IngestionTemplateResponse;
import com.uab.core.exception.BadRequestException;
import com.uab.core.exception.ResourceNotFoundException;
import com.uab.salesprevision.template.entity.IngestionTemplate;
import com.uab.salesprevision.template.mappers.TemplateEntitiesMapper;
import com.uab.salesprevision.template.repository.IngestionTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IngestionTemplateService {

    private final IngestionTemplateRepository ingestionTemplateRepository;
    private final TemplateEntitiesMapper templateEntitiesMapper;

    @Transactional
    public IngestionTemplateResponse create(CreateIngestionTemplateRequest request) {
        validateCreateRequest(request);
        IngestionTemplate template = templateEntitiesMapper.createRequestToIngestionTemplate(request);
        IngestionTemplate saved = ingestionTemplateRepository.save(template);
        return templateEntitiesMapper.ingestionTemplateToIngestionTemplateResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<IngestionTemplateResponse> findAll() {
        return ingestionTemplateRepository.findAll().stream()
                .map(templateEntitiesMapper::ingestionTemplateToIngestionTemplateResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public IngestionTemplateResponse findById(Long id) {
        return templateEntitiesMapper.ingestionTemplateToIngestionTemplateResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public IngestionTemplate getEntity(Long id) {
        return ingestionTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("error.template.not.found", id));
    }

    private void validateCreateRequest(CreateIngestionTemplateRequest request) {
        if (ingestionTemplateRepository.existsByName(request.getName().trim())) {
            throw new BadRequestException("error.template.name.duplicate", request.getName());
        }

        if (request.getFields() == null || request.getFields().isEmpty()) {
            throw new BadRequestException("error.template.no.fields");
        }

        for (CreateIngestionTemplateRequest.FieldRequest field : request.getFields()) {
            if (!StringUtils.hasText(field.getFieldName())) {
                throw new BadRequestException("error.template.field.name.empty");
            }

            if (request.getFileType().name().equals("CSV")
                    && Boolean.FALSE.equals(request.getHasHeader())
                    && field.getPositionIndex() == null) {
                throw new BadRequestException("error.template.csv.position.index");
            }
        }
    }
}
