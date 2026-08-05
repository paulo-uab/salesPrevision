package com.uab.salesprevision.template.service;


import com.uab.salesprevision.template.dto.CreateIngestionTemplateRequest;
import com.uab.salesprevision.template.dto.IngestionTemplateResponse;
import com.uab.salesprevision.template.dto.UpdateIngestionTemplateRequest;
import com.uab.core.exception.BadRequestException;
import com.uab.core.exception.ResourceNotFoundException;
import com.uab.salesprevision.template.model.IngestionTemplate;
import com.uab.salesprevision.template.model.TemplateField;
import com.uab.salesprevision.template.model.TemplateValidationRule;
import com.uab.salesprevision.template.mappers.TemplateEntitiesMapper;
import com.uab.salesprevision.template.repository.IngestionTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionTemplateService {

    private final IngestionTemplateRepository ingestionTemplateRepository;
    private final TemplateEntitiesMapper templateEntitiesMapper;
    private final IngestionTemplateValidationService ingestionTemplateValidationService;

    @Transactional
    public IngestionTemplateResponse create(CreateIngestionTemplateRequest request) {
        Long companyId = currentCompanyId();
        log.info("Creating template: name='{}', fileType={}, companyId={}", request.getName(), request.getFileType(), companyId);
        validateNameAvailable(request.getName(), null, companyId);
        ingestionTemplateValidationService.validateFields(request.getFileType(), request.getHasHeader(), request.getFields());

        IngestionTemplate template = templateEntitiesMapper.createRequestToIngestionTemplate(request);
        template.setCompanyId(companyId);
        IngestionTemplate saved;
        try {
            saved = ingestionTemplateRepository.saveAndFlush(template);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate template name detected on save: '{}'", request.getName());
            throw new BadRequestException("error.template.name.duplicate", request.getName());
        }
        log.info("Template saved: id={}, name='{}', fields={}", saved.getId(), saved.getName(),
                saved.getFields() != null ? saved.getFields().size() : 0);
        return templateEntitiesMapper.ingestionTemplateToIngestionTemplateResponse(saved);
    }

    @Transactional
    public IngestionTemplateResponse update(Long id, UpdateIngestionTemplateRequest request) {
        Long companyId = currentCompanyId();
        log.info("Updating template id={}: name='{}', fileType={}, companyId={}", id, request.getName(), request.getFileType(), companyId);
        IngestionTemplate existing = getEntity(id, companyId);
        validateNameAvailable(request.getName(), id, companyId);
        ingestionTemplateValidationService.validateFields(request.getFileType(), request.getHasHeader(), request.getFields());

        existing.setName(request.getName().trim());
        existing.setDescription(request.getDescription());
        existing.setFileType(request.getFileType());
        existing.setDelimiter(request.getDelimiter());
        existing.setHasHeader(request.getHasHeader());
        existing.setSheetName(request.getSheetName());
        existing.setRootPath(request.getRootPath());
        existing.setVersion(existing.getVersion() == null ? 1 : existing.getVersion() + 1);

        existing.getFields().clear();
        for (CreateIngestionTemplateRequest.FieldRequest fieldRequest : request.getFields()) {
            TemplateField field = templateEntitiesMapper.fieldRequestToTemplateField(fieldRequest);
            field.setTemplate(existing);
            for (TemplateValidationRule rule : field.getRules()) {
                rule.setField(field);
            }
            existing.getFields().add(field);
        }

        IngestionTemplate saved;
        try {
            saved = ingestionTemplateRepository.saveAndFlush(existing);
        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate template name detected on update: '{}'", request.getName());
            throw new BadRequestException("error.template.name.duplicate", request.getName());
        }
        log.info("Template updated: id={}, name='{}', version={}", saved.getId(), saved.getName(), saved.getVersion());
        return templateEntitiesMapper.ingestionTemplateToIngestionTemplateResponse(saved);
    }

    @Transactional
    public IngestionTemplateResponse updateActiveStatus(Long id, boolean active) {
        IngestionTemplate existing = getEntity(id, currentCompanyId());
        existing.setActive(active);
        IngestionTemplate saved = ingestionTemplateRepository.save(existing);
        log.info("Template id={} active status set to {}", id, active);
        return templateEntitiesMapper.ingestionTemplateToIngestionTemplateResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<IngestionTemplateResponse> findAll(Boolean active, Pageable pageable) {
        Long companyId = currentCompanyId();
        log.debug("Fetching templates: companyId={}, active={}, page={}", companyId, active, pageable);
        Page<IngestionTemplate> page = active != null
                ? ingestionTemplateRepository.findByCompanyIdAndActive(companyId, active, pageable)
                : ingestionTemplateRepository.findByCompanyId(companyId, pageable);
        return page.map(templateEntitiesMapper::ingestionTemplateToIngestionTemplateResponse);
    }

    @Transactional(readOnly = true)
    public IngestionTemplateResponse findById(Long id) {
        log.debug("Fetching template by id={}", id);
        return templateEntitiesMapper.ingestionTemplateToIngestionTemplateResponse(getEntity(id, currentCompanyId()));
    }

    @Transactional(readOnly = true)
    public IngestionTemplate getEntity(Long id, Long companyId) {
        return ingestionTemplateRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> {
                    log.warn("Template not found: id={}, companyId={}", id, companyId);
                    return new ResourceNotFoundException("error.template.not.found", id);
                });
    }

    private void validateNameAvailable(String name, Long excludingId, Long companyId) {
        ingestionTemplateRepository.findByNameAndCompanyId(name.trim(), companyId)
                .filter(existing -> excludingId == null || !existing.getId().equals(excludingId))
                .ifPresent(existing -> {
                    log.warn("Duplicate template name: '{}', companyId={}", name, companyId);
                    throw new BadRequestException("error.template.name.duplicate", name);
                });
    }

    private Long currentCompanyId() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Object companyId = jwt.getClaim("companyId");
        if (!(companyId instanceof Number number)) {
            throw new BadRequestException("error.template.company.missing");
        }
        return number.longValue();
    }
}
