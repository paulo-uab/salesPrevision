package com.uab.salesprevision.ingestion.service;



import com.uab.salesprevision.ingestion.client.dto.TemplateClientDto;
import com.uab.salesprevision.ingestion.dto.CreateIngestionJobResponse;
import com.uab.salesprevision.ingestion.dto.IngestedRecordResponse;
import com.uab.salesprevision.ingestion.dto.IngestionErrorResponse;
import com.uab.salesprevision.ingestion.dto.IngestionJobResponse;
import com.uab.salesprevision.ingestion.client.TemplateClient;
import com.uab.salesprevision.ingestion.model.IngestionJob;
import com.uab.core.enums.FileType;
import com.uab.core.enums.IngestionStatus;
import com.uab.core.exception.BadRequestException;
import com.uab.core.exception.ResourceNotFoundException;
import com.uab.salesprevision.ingestion.kafka.IngestionEventProducer;
import com.uab.salesprevision.ingestion.mappers.IngestionEntitiesMapper;
import com.uab.salesprevision.ingestion.repository.IngestedRecordRepository;
import com.uab.salesprevision.ingestion.repository.IngestionErrorRepository;
import com.uab.salesprevision.ingestion.repository.IngestionJobRepository;
import com.uab.salesprevision.ingestion.service.ingestion.IngestionProcessor;
import com.uab.salesprevision.ingestion.service.ingestion.IngestionProcessorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class IngestionService {

    private final IngestionJobRepository ingestionJobRepository;
    private final IngestedRecordRepository ingestedRecordRepository;
    private final IngestionErrorRepository ingestionErrorRepository;
    private final TemplateClient templateClient;
    private final IngestionProcessorFactory ingestionProcessorFactory;
    private final IngestionEntitiesMapper ingestionEntitiesMapper;
    private final IngestionEventProducer eventProducer;
    private final Path storageRoot;

    public IngestionService(IngestionJobRepository ingestionJobRepository,
                            IngestedRecordRepository ingestedRecordRepository,
                            IngestionErrorRepository ingestionErrorRepository,
                            TemplateClient templateClient,
                            IngestionProcessorFactory ingestionProcessorFactory,
                            IngestionEntitiesMapper ingestionEntitiesMapper,
                            IngestionEventProducer eventProducer,
                            @Value("${app.storage.upload-dir:uploads}") String uploadDir) {
        this.ingestionJobRepository = ingestionJobRepository;
        this.ingestedRecordRepository = ingestedRecordRepository;
        this.ingestionErrorRepository = ingestionErrorRepository;
        this.templateClient = templateClient;
        this.ingestionProcessorFactory = ingestionProcessorFactory;
        this.ingestionEntitiesMapper = ingestionEntitiesMapper;
        this.eventProducer = eventProducer;
        this.storageRoot = Paths.get(uploadDir);
        initStorage();
    }

    @Transactional
    public CreateIngestionJobResponse createJob(MultipartFile file,
                                                Long templateId,
                                                String createdBy,
                                                boolean autoProcess) {

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("error.ingestion.file.required");
        }

        Long companyId = currentCompanyId();
        log.info("Creating job: templateId={}, file='{}', createdBy='{}', autoProcess={}, companyId={}",
                templateId, file.getOriginalFilename(), createdBy, autoProcess, companyId);

        TemplateClientDto template = templateClient.getTemplate(templateId);

        String originalFileName = file.getOriginalFilename() != null
                ? file.getOriginalFilename()
                : "uploaded_file";

        FileType detectedFileType = detectFileType(originalFileName);

        if (template.getFileType() != FileType.UNKNOWN
                && detectedFileType != FileType.UNKNOWN
                && template.getFileType() != detectedFileType) {
            log.warn("File type mismatch: expected={}, detected={}, file='{}'",
                    template.getFileType(), detectedFileType, originalFileName);
            throw new BadRequestException("error.ingestion.file.type.mismatch");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("error.ingestion.file.read");
        }

        String storedFileName = UUID.randomUUID() + "_" + sanitizeFileName(originalFileName);
        Path templateDir = storageRoot.resolve("template_" + templateId);
        Path targetPath = templateDir.resolve(storedFileName);

        try {
            Files.createDirectories(templateDir);
            Files.write(targetPath, bytes);
        } catch (IOException e) {
            throw new BadRequestException("error.ingestion.file.save", e.getMessage());
        }

        IngestionJob job = IngestionJob.builder()
                .companyId(companyId)
                .templateId(templateId)
                .templateName(template.getName())
                .status(IngestionStatus.RECEIVED)
                .originalFileName(originalFileName)
                .storedFileName(storedFileName)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .storagePath(targetPath.toAbsolutePath().toString())
                .checksum(sha256Hex(bytes))
                .createdBy(createdBy)
                .build();

        IngestionJob saved = ingestionJobRepository.save(job);
        log.info("Job created: id={}, file='{}', size={}B, checksum={}",
                saved.getId(), originalFileName, file.getSize(), saved.getChecksum());

        if (autoProcess) {
            eventProducer.publish(saved.getId());
        }

        return ingestionEntitiesMapper.ingestionJobToCreateIngestionJobResponse(saved);
    }

    @Transactional
    public IngestionJobResponse processJob(Long jobId) {
        log.info("Processing job: id={}", jobId);
        IngestionJob job = getJobEntity(jobId);

        TemplateClientDto template = templateClient.getTemplate(job.getTemplateId());
        FileType fileType = template.getFileType();
        if (fileType == null || fileType == FileType.UNKNOWN) {
            fileType = detectFileType(job.getOriginalFileName());
        }

        IngestionProcessor processor = ingestionProcessorFactory.getProcessor(fileType);
        processor.process(job, template);

        IngestionJobResponse response = ingestionEntitiesMapper.ingestionJobToIngestionJobResponse(getJobEntity(jobId));
        log.info("Job processed: id={}, status={}", jobId, response.getStatus());
        return response;
    }

    public void submitForProcessing(Long jobId) {
        ensureJobExists(jobId, currentCompanyId());
        log.info("Submitting job id={} for async processing", jobId);
        eventProducer.publish(jobId);
    }

    @Transactional
    public void markJobFailed(Long jobId, String errorMessage) {
        log.warn("Marking job id={} as FAILED: {}", jobId, errorMessage);
        ingestionJobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(IngestionStatus.FAILED);
            job.setErrorMessage(errorMessage);
            job.setFinishedAt(LocalDateTime.now());
            ingestionJobRepository.save(job);
        });
    }

    @Transactional(readOnly = true)
    public List<IngestionJobResponse> findAll(Long templateId, IngestionStatus status) {
        Long companyId = currentCompanyId();
        log.debug("Listing jobs: companyId={}, templateId={}, status={}", companyId, templateId, status);
        List<IngestionJob> jobs;

        if (templateId != null && status != null) {
            jobs = ingestionJobRepository.findByCompanyIdAndTemplateIdAndStatusOrderByCreatedAtDesc(companyId, templateId, status);
        } else if (templateId != null) {
            jobs = ingestionJobRepository.findByCompanyIdAndTemplateIdOrderByCreatedAtDesc(companyId, templateId);
        } else if (status != null) {
            jobs = ingestionJobRepository.findByCompanyIdAndStatusOrderByCreatedAtDesc(companyId, status);
        } else {
            jobs = ingestionJobRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
        }

        log.debug("Found {} jobs", jobs.size());
        return jobs.stream()
                .map(ingestionEntitiesMapper::ingestionJobToIngestionJobResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public IngestionJobResponse findById(Long jobId) {
        log.debug("Fetching job id={}", jobId);
        return ingestionEntitiesMapper.ingestionJobToIngestionJobResponse(getJobEntity(jobId, currentCompanyId()));
    }

    @Transactional(readOnly = true)
    public Page<IngestedRecordResponse> findRecords(Long jobId, Pageable pageable) {
        ensureJobExists(jobId, currentCompanyId());
        log.debug("Fetching records: jobId={}, page={}", jobId, pageable.getPageNumber());
        return ingestedRecordRepository
                .findByIngestionJobIdOrderByRecordIndexAsc(jobId, pageable)
                .map(ingestionEntitiesMapper::ingestedRecordToIngestedRecordResponse);
    }

    @Transactional(readOnly = true)
    public Page<IngestionErrorResponse> findErrors(Long jobId, Pageable pageable) {
        ensureJobExists(jobId, currentCompanyId());
        log.debug("Fetching errors: jobId={}, page={}", jobId, pageable.getPageNumber());
        return ingestionErrorRepository
                .findByIngestionJobIdOrderByCreatedAtAsc(jobId, pageable)
                .map(ingestionEntitiesMapper::ingestionErrorToIngestionErrorResponse);
    }

    /** Used only by internal, non-HTTP callers (the Kafka consumer) — no caller identity to scope by. */
    private IngestionJob getJobEntity(Long jobId) {
        return ingestionJobRepository.findById(jobId)
                .orElseThrow(() -> {
                    log.warn("Job not found: id={}", jobId);
                    return new ResourceNotFoundException("error.ingestion.job.not.found", jobId);
                });
    }

    private IngestionJob getJobEntity(Long jobId, Long companyId) {
        return ingestionJobRepository.findByIdAndCompanyId(jobId, companyId)
                .orElseThrow(() -> {
                    log.warn("Job not found: id={}, companyId={}", jobId, companyId);
                    return new ResourceNotFoundException("error.ingestion.job.not.found", jobId);
                });
    }

    private void ensureJobExists(Long jobId, Long companyId) {
        if (ingestionJobRepository.findByIdAndCompanyId(jobId, companyId).isEmpty()) {
            log.warn("Job not found: id={}, companyId={}", jobId, companyId);
            throw new ResourceNotFoundException("error.ingestion.job.not.found", jobId);
        }
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
                    throw new BadRequestException("error.ingestion.company.missing");
                }
            }
        }

        Object companyId = jwt.getClaim("companyId");
        if (!(companyId instanceof Number number)) {
            throw new BadRequestException("error.ingestion.company.missing");
        }
        return number.longValue();
    }

    private FileType detectFileType(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return FileType.UNKNOWN;
        }

        String lower = fileName.toLowerCase();

        if (lower.endsWith(".csv")) return FileType.CSV;
        if (lower.endsWith(".json")) return FileType.JSON;
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) return FileType.XLSX;
        if (lower.endsWith(".txt")) return FileType.TXT;

        return FileType.UNKNOWN;
    }

    private String sanitizeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "file";
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private void initStorage() {
        try {
            Files.createDirectories(storageRoot);
            log.info("Upload directory ready: {}", storageRoot.toAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialise upload directory: " + storageRoot, e);
        }
    }
}
