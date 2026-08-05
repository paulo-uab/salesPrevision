package com.uab.salesprevision.batch.engine;

import com.uab.salesprevision.batch.client.dto.PipelineClientDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TransformationEngine {

    private final ObjectMapper objectMapper;

    public Map<String, Object> apply(Map<String, Object> rawRecord, List<PipelineClientDto.FieldDto> fields) {
        Map<String, Object> result = new LinkedHashMap<>();

        List<PipelineClientDto.FieldDto> activeFields = fields.stream()
                .filter(f -> Boolean.TRUE.equals(f.getActive()))
                .sorted(Comparator.comparingInt(f -> f.getPositionIndex() == null ? Integer.MAX_VALUE : f.getPositionIndex()))
                .toList();

        for (PipelineClientDto.FieldDto field : activeFields) {
            Object value = rawRecord.get(field.getSourceFieldName());
            String transformed = value == null ? null : String.valueOf(value);
            transformed = applyTransformation(field, transformed);
            result.put(field.getTargetFieldName(), transformed);
        }

        return result;
    }

    private String applyTransformation(PipelineClientDto.FieldDto field, String value) {
        if (field.getTransformationType() == null) {
            return value;
        }
        return switch (field.getTransformationType()) {
            case NONE, RENAME -> value;
            case SCALE -> applyScale(value, field.getTransformationConfig());
            case DATE_FORMAT -> applyDateFormat(value, field.getTransformationConfig());
            case CAST -> value;
            case REPLACE -> applyReplace(value, field.getTransformationConfig());
        };
    }

    private String applyScale(String value, String config) {
        if (!StringUtils.hasText(value) || !StringUtils.hasText(config)) {
            return value;
        }
        try {
            Map<String, Object> cfg = parseConfig(config);
            BigDecimal factor = new BigDecimal(String.valueOf(cfg.getOrDefault("factor", "1")));
            return new BigDecimal(value).multiply(factor).toPlainString();
        } catch (Exception e) {
            return value;
        }
    }

    private String applyDateFormat(String value, String config) {
        if (!StringUtils.hasText(value) || !StringUtils.hasText(config)) {
            return value;
        }
        try {
            Map<String, Object> cfg = parseConfig(config);
            String fromPattern = String.valueOf(cfg.getOrDefault("from", "yyyy-MM-dd"));
            String toPattern = String.valueOf(cfg.getOrDefault("to", "yyyy-MM-dd"));
            LocalDate date = LocalDate.parse(value, DateTimeFormatter.ofPattern(fromPattern));
            return date.format(DateTimeFormatter.ofPattern(toPattern));
        } catch (Exception e) {
            return value;
        }
    }

    private String applyReplace(String value, String config) {
        if (!StringUtils.hasText(value) || !StringUtils.hasText(config)) {
            return value;
        }
        try {
            Map<String, Object> cfg = parseConfig(config);
            String regex = String.valueOf(cfg.getOrDefault("regex", ""));
            String replacement = String.valueOf(cfg.getOrDefault("replacement", ""));
            return StringUtils.hasText(regex) ? value.replaceAll(regex, replacement) : value;
        } catch (Exception e) {
            return value;
        }
    }

    private Map<String, Object> parseConfig(String json) throws Exception {
        return objectMapper.readValue(json, new TypeReference<>() {});
    }
}
