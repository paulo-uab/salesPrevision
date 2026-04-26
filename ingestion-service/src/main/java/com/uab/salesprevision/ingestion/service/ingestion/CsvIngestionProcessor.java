package com.uab.salesprevision.ingestion.service.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uab.core.dto.ingestion.TemplateDto;
import com.uab.salesprevision.ingestion.entity.IngestedRecord;
import com.uab.salesprevision.ingestion.entity.IngestionJob;
import com.uab.core.enums.FileType;
import com.uab.core.enums.ValidationStatus;
import com.uab.salesprevision.ingestion.repository.IngestedRecordRepository;
import com.uab.salesprevision.ingestion.repository.IngestionErrorRepository;
import com.uab.salesprevision.ingestion.repository.IngestionJobRepository;
import com.uab.salesprevision.ingestion.service.IngestionRecordFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class CsvIngestionProcessor extends AbstractIngestionProcessor {

    public CsvIngestionProcessor(IngestionJobRepository ingestionJobRepository,
                                 IngestedRecordRepository ingestedRecordRepository,
                                 IngestionErrorRepository ingestionErrorRepository,
                                 IngestionRecordFactory ingestionRecordFactory,
                                 ObjectMapper objectMapper) {
        super(ingestionJobRepository, ingestedRecordRepository, ingestionErrorRepository,
                ingestionRecordFactory, objectMapper);
    }

    @Override
    public FileType supports() {
        return FileType.CSV;
    }

    @Override
    protected void doProcess(IngestionJob job, TemplateDto template) {
        String content = readFileContent(job);
        List<String> lines = splitLines(content);

        if (lines.isEmpty()) {
            throw new IllegalArgumentException("O ficheiro CSV está vazio");
        }

        String delimiter = resolveDelimiter(template.getDelimiter());
        CsvContext context = prepareContext(template, lines, delimiter);

        long recordCount = 0L;
        long errorCount = 0L;

        List<TemplateDto.FieldDto> activeFields = template.getFields().stream()
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

            for (TemplateDto.FieldDto field : activeFields) {
                String rawValue = resolveRawValue(field, row, context.headerIndexMap());
                rawValue = applyDefaultIfNecessary(rawValue, field.getDefaultValue());

                try {
                    Object converted = convertAndValidateField(field, rawValue);
                    normalizedPayload.put(field.getFieldName(), converted);
                } catch (FieldValidationException ex) {
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
    }

    private CsvContext prepareContext(TemplateDto template, List<String> lines, String delimiter) {
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

    private String resolveRawValue(TemplateDto.FieldDto field, List<String> row, Map<String, Integer> headerIndexMap) {
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
