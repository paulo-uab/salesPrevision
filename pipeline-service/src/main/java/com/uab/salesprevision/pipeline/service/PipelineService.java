package com.uab.salesprevision.pipeline.service;

import com.uab.salesprevision.pipeline.client.dto.TemplateClientDto;
import com.uab.salesprevision.pipeline.dto.PipelineDto;
import com.uab.core.exception.BadRequestException;
import com.uab.core.exception.ResourceNotFoundException;
import com.uab.salesprevision.pipeline.client.TemplateClient;
import com.uab.salesprevision.pipeline.dto.CreateForecastPipelineRequest;
import com.uab.salesprevision.pipeline.dto.ForecastPipelineResponse;
import com.uab.salesprevision.pipeline.model.ForecastPipeline;
import com.uab.salesprevision.pipeline.model.PipelineField;
import com.uab.salesprevision.pipeline.model.PipelineFilter;
import com.uab.salesprevision.pipeline.mapper.PipelineMapper;
import com.uab.salesprevision.pipeline.repository.ForecastPipelineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineService {

    private final ForecastPipelineRepository repository;
    private final TemplateClient templateClient;
    private final PipelineMapper mapper;

    @Transactional
    public ForecastPipelineResponse create(CreateForecastPipelineRequest request) {
        Long companyId = currentCompanyId();
        log.info("Creating pipeline: name='{}', templateId={}, companyId={}", request.getName(), request.getTemplateId(), companyId);

        if (repository.existsByNameAndCompanyId(request.getName(), companyId)) {
            log.warn("Duplicate pipeline name: '{}'", request.getName());
            throw new BadRequestException("error.pipeline.name.duplicate", request.getName());
        }

        TemplateClientDto template = templateClient.getTemplate(request.getTemplateId());

        ForecastPipeline pipeline = mapper.toEntity(request);
        pipeline.setCompanyId(companyId);
        pipeline.setTemplateName(template.getName());

        List<PipelineField> fields = nullSafe(request.getFields()).stream()
                .map(mapper::toFieldEntity)
                .toList();
        fields.forEach(f -> f.setPipeline(pipeline));
        pipeline.getFields().addAll(fields);

        List<PipelineFilter> filters = nullSafe(request.getFilters()).stream()
                .map(mapper::toFilterEntity)
                .toList();
        filters.forEach(f -> f.setPipeline(pipeline));
        pipeline.getFilters().addAll(filters);

        ForecastPipeline saved = repository.save(pipeline);
        log.info("Pipeline created: id={}, name='{}', fields={}, filters={}",
                saved.getId(), saved.getName(), fields.size(), filters.size());
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ForecastPipelineResponse> findAll(Long templateId) {
        Long companyId = currentCompanyId();
        log.debug("Listing pipelines: companyId={}, templateId={}", companyId, templateId);
        List<ForecastPipeline> pipelines = templateId != null
                ? repository.findByCompanyIdAndTemplateIdOrderByCreatedAtDesc(companyId, templateId)
                : repository.findByCompanyId(companyId);
        log.debug("Found {} pipelines", pipelines.size());
        return mapper.toResponseList(pipelines);
    }

    @Transactional(readOnly = true)
    public ForecastPipelineResponse findById(Long id) {
        log.debug("Fetching pipeline id={}", id);
        return mapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public PipelineDto getDto(Long id) {
        log.debug("Fetching pipeline DTO id={}", id);
        return mapper.toDto(getEntity(id));
    }

    private ForecastPipeline getEntity(Long id) {
        Long companyId = currentCompanyId();
        return repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> {
                    log.warn("Pipeline not found: id={}, companyId={}", id, companyId);
                    return new ResourceNotFoundException("error.pipeline.not.found", id);
                });
    }

    private <T> List<T> nullSafe(List<T> list) {
        return list != null ? list : Collections.emptyList();
    }

    private Long currentCompanyId() {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // batch-service's cron-triggered calls have no end-user JWT to carry the
        // right companyId — they authenticate as this fixed service account and
        // pass the owning schedule's companyId explicitly instead. The header is
        // trusted only because the JWT subject itself is cryptographically verified.
        if ("internal-service".equals(jwt.getSubject())
                && RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String header = attributes.getRequest().getHeader("X-Company-Id");
            if (StringUtils.hasText(header)) {
                try {
                    return Long.parseLong(header);
                } catch (NumberFormatException e) {
                    throw new BadRequestException("error.pipeline.company.missing");
                }
            }
        }

        Object companyId = jwt.getClaim("companyId");
        if (!(companyId instanceof Number number)) {
            throw new BadRequestException("error.pipeline.company.missing");
        }
        return number.longValue();
    }
}
