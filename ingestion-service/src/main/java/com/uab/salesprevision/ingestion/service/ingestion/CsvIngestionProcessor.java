package com.uab.salesprevision.ingestion.service.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uab.salesprevision.ingestion.client.dto.TemplateClientDto;
import com.uab.salesprevision.ingestion.model.IngestedRecord;
import com.uab.salesprevision.ingestion.model.IngestionJob;
import com.uab.core.enums.FileType;
import com.uab.core.enums.ValidationStatus;
import com.uab.core.exception.BadRequestException;
import com.uab.salesprevision.ingestion.repository.IngestedRecordRepository;
import com.uab.salesprevision.ingestion.repository.IngestionErrorRepository;
import com.uab.salesprevision.ingestion.repository.IngestionJobRepository;
import com.uab.salesprevision.ingestion.service.IngestionRecordFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Slf4j
@Service
public class CsvIngestionProcessor extends AbstractIngestionProcessor {

    public CsvIngestionProcessor(IngestionJobRepository ingestionJobRepository,
                                 IngestedRecordRepository ingestedRecordRepository,
                                 IngestionErrorRepository ingestionErrorRepository,
                                 IngestionRecordFactory ingestionRecordFactory,
                                 ObjectMapper objectMapper,
                                 MessageSource messageSource) {
        super(ingestionJobRepository, ingestedRecordRepository, ingestionErrorRepository,
                ingestionRecordFactory, objectMapper, messageSource);
    }

    @Override
    public FileType supports() {
        return FileType.CSV;
    }

    @Override
    protected void doProcess(IngestionJob job, TemplateClientDto template) {
        String content = readFileContent(job);
        List<String> lines = splitLines(content);

        if (lines.isEmpty()) {
            throw new BadRequestException("error.ingestion.csv.empty");
        }

        String delimiter = resolveDelimiter(template.getDelimiter());
        CsvContext context = prepareContext(template, lines, delimiter);

        log.debug("CSV context: jobId={}, file={}, lines={}, hasHeader={}, delimiter='{}'",
                job.getId(), job.getStoredFileName(), lines.size(), template.getHasHeader(), delimiter);

        long recordCount = 0L;
        long errorCount = 0L;

        List<TemplateClientDto.FieldDto> activeFields = template.getFields().stream()
                .filter(field -> Boolean.TRUE.equals(field.getActive()))
                .sorted(Comparator.comparing(field ->
                        field.getPositionIndex() == null ? Integer.MAX_VALUE : field.getPositionIndex()))
                .toList();

        for (int lineIdx = context.dataStartIndex(); lineIdx < lines.size(); lineIdx++) {
            String line = lines.get(lineIdx);
            if (!StringUtils.hasText(line)) {
                continue;
            }

            recordCount++;
            long lineNumber = lineIdx + 1L;
            List<String> row = parseCsvLine(line, delimiter);

            Map<String, Object> rawPayload = buildRawPayload(context.header(), row);
            Map<String, Object> normalizedPayload = new LinkedHashMap<>();
            List<RowError> rowErrors = new ArrayList<>();

            for (TemplateClientDto.FieldDto field : activeFields) {
                String rawValue = resolveRawValue(field, row, context.headerIndexMap());
                rawValue = applyDefaultIfNecessary(rawValue, field.getDefaultValue());

                try {
                    Object converted = convertAndValidateField(field, rawValue);
                    normalizedPayload.put(field.getFieldName(), converted);
                } catch (FieldValidationException ex) {
                    log.debug("Validation error: jobId={}, line={}, field='{}', type={}, value='{}'",
                            job.getId(), lineNumber, field.getFieldName(), ex.getErrorType(), rawValue);
                    rowErrors.add(new RowError(field.getFieldName(), ex.getErrorType(), ex.getMessage(), rawValue, lineNumber));
                }
            }

            IngestedRecord record = saveRecord(
                    job,
                    recordCount,
                    rawPayload,
                    normalizedPayload,
                    rowErrors.isEmpty() ? ValidationStatus.VALID : ValidationStatus.INVALID
            );

            for (RowError error : rowErrors) {
                saveError(job, record, error.fieldName(), error.errorType(), error.message(), error.rawValue(), error.lineNumber());
                errorCount++;
            }
        }

        job.setRecordCount(recordCount);
        job.setErrorCount(errorCount);
        log.info("CSV processing complete: jobId={}, records={}, errors={}", job.getId(), recordCount, errorCount);
    }

    private CsvContext prepareContext(TemplateClientDto template, List<String> lines, String delimiter) {
        List<String> header = new ArrayList<>();
        Map<String, Integer> headerIndexMap = new LinkedHashMap<>();
        int dataStartIndex = 0;

        if (Boolean.TRUE.equals(template.getHasHeader())) {
            header = parseCsvLine(lines.get(0), delimiter).stream()
                    .map(String::trim)
                    .toList();

            for (int i = 0; i < header.size(); i++) {
                headerIndexMap.put(header.get(i), i);
            }

            dataStartIndex = 1;
        }

        return new CsvContext(header, headerIndexMap, dataStartIndex);
    }

    private String resolveRawValue(TemplateClientDto.FieldDto field, List<String> row, Map<String, Integer> headerIndexMap) {
        Integer index = null;

        if (!headerIndexMap.isEmpty() && StringUtils.hasText(field.getSourceName())) {
            index = headerIndexMap.get(field.getSourceName());
        }

        if (index == null && field.getPositionIndex() != null) {
            index = field.getPositionIndex();
        }

        if (index == null || index < 0 || index >= row.size()) {
            return null;
        }

        return row.get(index);
    }

    private Map<String, Object> buildRawPayload(List<String> header, List<String> row) {
        Map<String, Object> payload = new LinkedHashMap<>();

        if (header != null && !header.isEmpty()) {
            for (int i = 0; i < row.size(); i++) {
                String key = i < header.size() ? header.get(i) : "column_" + i;
                payload.put(key, row.get(i));
            }
        } else {
            for (int i = 0; i < row.size(); i++) {
                payload.put("column_" + i, row.get(i));
            }
        }

        return payload;
    }

    private List<String> splitLines(String content) {
        return content.lines().toList();
    }

    private String resolveDelimiter(String configuredDelimiter) {
        if (!StringUtils.hasText(configuredDelimiter)) {
            return ",";
        }
        if ("\\t".equals(configuredDelimiter)) {
            return "\t";
        }
        return configuredDelimiter;
    }

    private List<String> parseCsvLine(String line, String delimiter) {
        return List.of(line.split(java.util.regex.Pattern.quote(delimiter), -1));
    }

    private record CsvContext(List<String> header, Map<String, Integer> headerIndexMap, int dataStartIndex) {}

    private record RowError(String fieldName, String errorType, String message, String rawValue, long lineNumber) {}
}
