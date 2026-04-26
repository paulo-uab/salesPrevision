package com.uab.salesprevision.ingestion.service;



import com.uab.core.dto.ingestion.*;
import com.uab.salesprevision.ingestion.entity.IngestionJob;
import com.uab.core.enums.FileType;
import com.uab.core.enums.IngestionStatus;
import com.uab.core.exception.BadRequestException;
import com.uab.core.exception.ResourceNotFoundException;
import com.uab.salesprevision.ingestion.mappers.IngestionEntitiesMapper;
import com.uab.salesprevision.ingestion.client.TemplateClient;
import com.uab.salesprevision.ingestion.repository.IngestedRecordRepository;
import com.uab.salesprevision.ingestion.repository.IngestionErrorRepository;
import com.uab.salesprevision.ingestion.repository.IngestionJobRepository;
import com.uab.salesprevision.ingestion.service.ingestion.IngestionProcessor;
import com.uab.salesprevision.ingestion.service.ingestion.IngestionProcessorFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import java.util.Comparator;

@Service
public class IngestionService {

    private final IngestionJobRepository ingestionJobRepository;
    private final IngestedRecordRepository ingestedRecordRepository;
    private final IngestionErrorRepository ingestionErrorRepository;
    private final TemplateClient templateClient;
    private final IngestionProcessorFactory ingestionProcessorFactory;
    private final IngestionEntitiesMapper ingestionEntitiesMapper;
    private final Path storageRoot;

    public IngestionService(IngestionJobRepository ingestionJobRepository,
                            IngestedRecordRepository ingestedRecordRepository,
                            IngestionErrorRepository ingestionErrorRepository,
                            TemplateClient templateClient,
                            IngestionProcessorFactory ingestionProcessorFactory,
                            IngestionEntitiesMapper ingestionEntitiesMapper,
                            @Value("${app.storage.upload-dir:uploads}") String uploadDir) {
        this.ingestionJobRepository = ingestionJobRepository;
        this.ingestedRecordRepository = ingestedRecordRepository;
        this.ingestionErrorRepository = ingestionErrorRepository;
        this.templateClient = templateClient;
        this.ingestionProcessorFactory = ingestionProcessorFactory;
        this.ingestionEntitiesMapper = ingestionEntitiesMapper;
        this.storageRoot = Paths.get(uploadDir);
        initStorage();
    }

    @Transactional
    public CreateIngestionJobResponse createJob(MultipartFile file,
                                                Long templateId,
                                                String createdBy,
                                                boolean autoProcess) {

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("É necessário enviar um ficheiro");
        }

        TemplateDto template = templateClient.getTemplate(templateId);

        String originalFileName = file.getOriginalFilename() != null
                ? file.getOriginalFilename()
                : "uploaded_file";

        FileType detectedFileType = detectFileType(originalFileName);

        if (template.getFileType() != FileType.UNKNOWN
                && detectedFileType != FileType.UNKNOWN
                && template.getFileType() != detectedFileType) {
            throw new BadRequestException("O ficheiro enviado não corresponde ao tipo configurado no template");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("Não foi possível ler o ficheiro enviado");
        }

        String storedFileName = UUID.randomUUID() + "_" + sanitizeFileName(originalFileName);
        Path templateDir = storageRoot.resolve("template_" + templateId);
        Path targetPath = templateDir.resolve(storedFileName);

        try {
            Files.createDirectories(templateDir);
            Files.write(targetPath, bytes);
        } catch (IOException e) {
            throw new BadRequestException("Não foi possível guardar o ficheiro: " + e.getMessage());
        }

        IngestionJob job = IngestionJob.builder()
                .templateId(templateId)
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

        if (autoProcess) {
            processJob(saved.getId());
            saved = getJobEntity(saved.getId());
        }

        return ingestionEntitiesMapper.ingestionJobToCreateIngestionJobResponse(saved);
    }

    @Transactional
    public IngestionJobResponse processJob(Long jobId) {
        IngestionJob job = getJobEntity(jobId);

        FileType fileType = job.getTemplate().getFileType();
        if (fileType == null || fileType == FileType.UNKNOWN) {
            fileType = detectFileType(job.getOriginalFileName());
        }

        IngestionProcessor processor = ingestionProcessorFactory.getProcessor(fileType);
        processor.process(job);

        return ingestionEntitiesMapper.ingestionJobToIngestionJobResponse(getJobEntity(jobId));
    }

    @Transactional(readOnly = true)
    public List<IngestionJobResponse> findAll(Long templateId, IngestionStatus status) {
        List<IngestionJob> jobs;

        if (templateId != null && status != null) {
            jobs = ingestionJobRepository.findByTemplateIdAndStatusOrderByCreatedAtDesc(templateId, status);
        } else if (templateId != null) {
            jobs = ingestionJobRepository.findByTemplateIdOrderByCreatedAtDesc(templateId);
        } else if (status != null) {
            jobs = ingestionJobRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            jobs = ingestionJobRepository.findAll().stream()
                    .sorted(Comparator.comparing(IngestionJob::getCreatedAt).reversed())
                    .toList();
        }

        return jobs.stream()
                .map(ingestionEntitiesMapper::ingestionJobToIngestionJobResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public IngestionJobResponse findById(Long jobId) {
        return ingestionEntitiesMapper.ingestionJobToIngestionJobResponse(getJobEntity(jobId));
    }

    @Transactional(readOnly = true)
    public Page<IngestedRecordResponse> findRecords(Long jobId, Pageable pageable) {
        ensureJobExists(jobId);
        return ingestedRecordRepository
                .findByIngestionJobIdOrderByRecordIndexAsc(jobId, pageable)
                .map(ingestionEntitiesMapper::ingestedRecordToIngestedRecordResponse);
    }

    @Transactional(readOnly = true)
    public Page<IngestionErrorResponse> findErrors(Long jobId, Pageable pageable) {
        ensureJobExists(jobId);
        return ingestionErrorRepository
                .findByIngestionJobIdOrderByCreatedAtAsc(jobId, pageable)
                .map(ingestionEntitiesMapper::ingestionErrorToIngestionErrorResponse);
    }

    private IngestionJob getJobEntity(Long jobId) {
        return ingestionJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Ingestion job não encontrado com id " + jobId));
    }

    private void ensureJobExists(Long jobId) {
        if (!ingestionJobRepository.existsById(jobId)) {
            throw new ResourceNotFoundException("Ingestion job não encontrado com id " + jobId);
        }
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
            throw new IllegalStateException("SHA-256 não disponível", e);
        }
    }

    private void initStorage() {
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível inicializar a diretoria de uploads", e);
        }
    }
}