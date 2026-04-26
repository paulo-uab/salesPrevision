package com.uab.salesprevision.ingestion.service.ingestion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uab.core.dto.ingestion.TemplateDto;
import com.uab.salesprevision.ingestion.entity.IngestedRecord;
import com.uab.salesprevision.ingestion.entity.IngestionJob;
import com.uab.core.enums.FieldDataType;
import com.uab.core.enums.IngestionStatus;
import com.uab.core.enums.TemplateRuleType;
import com.uab.core.enums.ValidationStatus;
import com.uab.core.exception.BadRequestException;
import com.uab.salesprevision.ingestion.repository.IngestedRecordRepository;
import com.uab.salesprevision.ingestion.repository.IngestionErrorRepository;
import com.uab.salesprevision.ingestion.repository.IngestionJobRepository;
import com.uab.salesprevision.ingestion.service.IngestionRecordFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static com.uab.core.enums.TemplateRuleType.*;

public abstract class AbstractIngestionProcessor implements IngestionProcessor {

    protected final IngestionJobRepository ingestionJobRepository;
    protected final IngestedRecordRepository ingestedRecordRepository;
    protected final IngestionErrorRepository ingestionErrorRepository;
    protected final IngestionRecordFactory ingestionRecordFactory;
    protected final ObjectMapper objectMapper;

    protected AbstractIngestionProcessor(IngestionJobRepository ingestionJobRepository,
                                         IngestedRecordRepository ingestedRecordRepository,
                                         IngestionErrorRepository ingestionErrorRepository,
                                         IngestionRecordFactory ingestionRecordFactory,
                                         ObjectMapper objectMapper) {
        this.ingestionJobRepository = ingestionJobRepository;
        this.ingestedRecordRepository = ingestedRecordRepository;
        this.ingestionErrorRepository = ingestionErrorRepository;
        this.ingestionRecordFactory = ingestionRecordFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void process(IngestionJob job, TemplateDto template) {
        resetJobProcessingState(job);

        job.setStatus(IngestionStatus.PROCESSING);
        job.setStartedAt(LocalDateTime.now());
        ingestionJobRepository.save(job);

        try {
            doProcess(job, template);

            job.setStatus(IngestionStatus.COMPLETED);
            job.setFinishedAt(LocalDateTime.now());
            job.setErrorMessage(null);
            ingestionJobRepository.save(job);
        } catch (Exception ex) {
            job.setStatus(IngestionStatus.FAILED);
            job.setFinishedAt(LocalDateTime.now());
            job.setErrorMessage(ex.getMessage());
            ingestionJobRepository.save(job);
            throw ex;
        }
    }

    protected abstract void doProcess(IngestionJob job, TemplateDto template);

    protected String readFileContent(IngestionJob job) {
        try {
            return Files.readString(Path.of(job.getStoragePath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BadRequestException("Não foi possível ler o ficheiro: " + e.getMessage());
        }
    }

    protected IngestedRecord saveRecord(IngestionJob job,
                                        long recordIndex,
                                        Map<String, Object> rawPayload,
                                        Map<String, Object> normalizedPayload,
                                        ValidationStatus validationStatus) {
        IngestedRecord record = ingestionRecordFactory.buildRecord(
                job,
                recordIndex,
                toJson(rawPayload),
                toJson(normalizedPayload),
                validationStatus
        );
        return ingestedRecordRepository.save(record);
    }

    protected void saveError(IngestionJob job,
                             IngestedRecord record,
                             String fieldName,
                             String errorType,
                             String message,
                             String rawValue,
                             Long lineNumber) {
        ingestionErrorRepository.save(
                ingestionRecordFactory.buildError(job, record, fieldName, errorType, message, rawValue, lineNumber)
        );
    }

    protected Object convertAndValidateField(TemplateDto.FieldDto field, String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            if (Boolean.TRUE.equals(field.getRequired()) || hasRule(field, NOT_NULL)) {
                throw new FieldValidationException("VALIDATION", "Campo obrigatório");
            }
            return null;
        }

        String trimmed = rawValue.trim();

        if (StringUtils.hasText(field.getValidationRegex()) && !trimmed.matches(field.getValidationRegex())) {
            throw new FieldValidationException("REGEX", "Valor não respeita o padrão configurado");
        }

        Object converted = convertValue(field, trimmed);
        applyRules(field, trimmed, converted);
        return converted;
    }

    protected Object convertValue(TemplateDto.FieldDto field, String value) {
        FieldDataType dataType = field.getDataType();

        try {
            return switch (dataType) {
                case STRING -> value;
                case INTEGER -> Integer.valueOf(value);
                case DECIMAL -> new BigDecimal(value);
                case BOOLEAN -> parseBoolean(value);
                case DATE -> parseDate(field.getDateFormat(), value).toString();
                default -> null;
            };
        } catch (NumberFormatException ex) {
            throw new FieldValidationException("TYPE_CONVERSION", "Não foi possível converter para " + dataType);
        } catch (DateTimeParseException ex) {
            throw new FieldValidationException("TYPE_CONVERSION", "Não foi possível converter para DATE");
        }
    }

    protected void applyRules(TemplateDto.FieldDto field, String rawValue, Object convertedValue) {
        if (field.getRules() == null) {
            return;
        }

        for (TemplateDto.RuleDto rule : field.getRules()) {
            if (!Boolean.TRUE.equals(rule.getActive())) {
                continue;
            }

            switch (rule.getRuleType()) {
                case NOT_NULL -> {
                    if (convertedValue == null) {
                        throw new FieldValidationException("NOT_NULL", defaultMessage(rule.getMessage(), "Campo obrigatório"));
                    }
                }
                case REGEX -> {
                    if (StringUtils.hasText(rule.getRuleValue()) && !rawValue.matches(rule.getRuleValue())) {
                        throw new FieldValidationException("REGEX", defaultMessage(rule.getMessage(), "Valor inválido"));
                    }
                }
                case MIN -> {
                    BigDecimal value = toBigDecimal(convertedValue);
                    BigDecimal min = new BigDecimal(rule.getRuleValue());
                    if (value.compareTo(min) < 0) {
                        throw new FieldValidationException("MIN", defaultMessage(rule.getMessage(), "Valor abaixo do mínimo"));
                    }
                }
                case MAX -> {
                    BigDecimal value = toBigDecimal(convertedValue);
                    BigDecimal max = new BigDecimal(rule.getRuleValue());
                    if (value.compareTo(max) > 0) {
                        throw new FieldValidationException("MAX", defaultMessage(rule.getMessage(), "Valor acima do máximo"));
                    }
                }
                case ENUM -> {
                    Set<String> accepted = splitEnumValues(rule.getRuleValue());
                    if (!accepted.contains(rawValue)) {
                        throw new FieldValidationException("ENUM", defaultMessage(rule.getMessage(), "Valor fora do conjunto permitido"));
                    }
                }
                case DATE_FORMAT -> parseDate(rule.getRuleValue(), rawValue);
            }
        }
    }

    protected String applyDefaultIfNecessary(String rawValue, String defaultValue) {
        if (StringUtils.hasText(rawValue)) {
            return rawValue;
        }
        return StringUtils.hasText(defaultValue) ? defaultValue : rawValue;
    }

    protected void resetJobProcessingState(IngestionJob job) {
        ingestionErrorRepository.deleteByIngestionJobId(job.getId());
        ingestedRecordRepository.deleteByIngestionJobId(job.getId());
        job.setRecordCount(0L);
        job.setErrorCount(0L);
        job.setErrorMessage(null);
        job.setFinishedAt(null);
    }

    protected String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Erro ao serializar payload");
        }
    }

    protected Map<String, Object> newPayload() {
        return new LinkedHashMap<>();
    }

    protected boolean hasRule(TemplateDto.FieldDto field, TemplateRuleType ruleType) {
        return field.getRules() != null && field.getRules().stream()
                .anyMatch(rule -> Boolean.TRUE.equals(rule.getActive()) && rule.getRuleType() == ruleType);
    }

    protected String defaultMessage(String customMessage, String fallback) {
        return StringUtils.hasText(customMessage) ? customMessage : fallback;
    }

    protected LocalDate parseDate(String format, String value) {
        if (StringUtils.hasText(format)) {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern(format));
        }
        return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    protected BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            throw new FieldValidationException("TYPE_CONVERSION", "Valor nulo");
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Integer integer) {
            return BigDecimal.valueOf(integer);
        }
        if (value instanceof Long longValue) {
            return BigDecimal.valueOf(longValue);
        }
        if (value instanceof String stringValue) {
            return new BigDecimal(stringValue);
        }
        throw new FieldValidationException("TYPE_CONVERSION", "Valor não numérico");
    }

    protected Boolean parseBoolean(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "true", "1", "yes", "y", "sim", "s" -> true;
            case "false", "0", "no", "n", "nao", "não" -> false;
            default -> throw new FieldValidationException("TYPE_CONVERSION", "Valor booleano inválido");
        };
    }

    protected Set<String> splitEnumValues(String raw) {
        Set<String> values = new HashSet<>();
        if (!StringUtils.hasText(raw)) {
            return values;
        }
        for (String part : raw.split(",")) {
            values.add(part.trim());
        }
        return values;
    }

    public static class FieldValidationException extends RuntimeException {
        private final String errorType;

        public FieldValidationException(String errorType, String message) {
            super(message);
            this.errorType = errorType;
        }

        public String getErrorType() {
            return errorType;
        }
    }
}
