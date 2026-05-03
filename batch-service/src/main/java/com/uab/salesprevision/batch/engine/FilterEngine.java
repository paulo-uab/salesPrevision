package com.uab.salesprevision.batch.engine;

import com.uab.core.dto.pipeline.PipelineDto;
import com.uab.core.enums.LogicalOperator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class FilterEngine {

    public boolean matches(Map<String, Object> record, List<PipelineDto.FilterDto> filters) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }

        List<PipelineDto.FilterDto> sorted = filters.stream()
                .sorted((a, b) -> {
                    int ia = a.getOrderIndex() == null ? Integer.MAX_VALUE : a.getOrderIndex();
                    int ib = b.getOrderIndex() == null ? Integer.MAX_VALUE : b.getOrderIndex();
                    return Integer.compare(ia, ib);
                })
                .toList();

        boolean result = evaluateFilter(record, sorted.get(0));

        for (int i = 1; i < sorted.size(); i++) {
            PipelineDto.FilterDto filter = sorted.get(i);
            PipelineDto.FilterDto previous = sorted.get(i - 1);
            boolean current = evaluateFilter(record, filter);

            if (previous.getLogicalOperator() == LogicalOperator.OR) {
                result = result || current;
            } else {
                result = result && current;
            }
        }

        return result;
    }

    private boolean evaluateFilter(Map<String, Object> record, PipelineDto.FilterDto filter) {
        Object fieldValue = record.get(filter.getFieldName());
        String rawValue = fieldValue == null ? null : String.valueOf(fieldValue);

        return switch (filter.getOperator()) {
            case IS_NULL -> fieldValue == null || !StringUtils.hasText(rawValue);
            case IS_NOT_NULL -> fieldValue != null && StringUtils.hasText(rawValue);
            case EQ -> rawValue != null && rawValue.equals(filter.getValue());
            case NEQ -> rawValue == null || !rawValue.equals(filter.getValue());
            case CONTAINS -> rawValue != null && rawValue.contains(filter.getValue());
            case IN -> rawValue != null && toSet(filter.getValue()).contains(rawValue);
            case NOT_IN -> rawValue == null || !toSet(filter.getValue()).contains(rawValue);
            case GT -> compareNumeric(rawValue, filter.getValue()) > 0;
            case GTE -> compareNumeric(rawValue, filter.getValue()) >= 0;
            case LT -> compareNumeric(rawValue, filter.getValue()) < 0;
            case LTE -> compareNumeric(rawValue, filter.getValue()) <= 0;
            case BETWEEN -> {
                String[] parts = filter.getValue().split(",", 2);
                if (parts.length != 2) yield false;
                yield compareNumeric(rawValue, parts[0].trim()) >= 0
                        && compareNumeric(rawValue, parts[1].trim()) <= 0;
            }
        };
    }

    private int compareNumeric(String rawValue, String compareValue) {
        try {
            return new BigDecimal(rawValue).compareTo(new BigDecimal(compareValue));
        } catch (NumberFormatException e) {
            return rawValue == null ? -1 : rawValue.compareTo(compareValue);
        }
    }

    private Set<String> toSet(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }
}
