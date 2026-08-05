package com.uab.salesprevision.template.service;

import com.uab.core.enums.FieldDataType;
import com.uab.core.enums.FileType;
import com.uab.core.enums.TemplateRuleType;
import com.uab.core.exception.BadRequestException;
import com.uab.salesprevision.template.dto.CreateIngestionTemplateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionTemplateValidationService {

    protected void validateFields(FileType fileType, Boolean hasHeader,
                                List<CreateIngestionTemplateRequest.FieldRequest> fields) {
        if (fields == null || fields.isEmpty()) {
            throw new BadRequestException("error.template.no.fields");
        }

        Set<String> seenFieldNames = new HashSet<>();
        for (CreateIngestionTemplateRequest.FieldRequest field : fields) {
            if (!StringUtils.hasText(field.getFieldName())) {
                throw new BadRequestException("error.template.field.name.empty");
            }

            String normalizedName = field.getFieldName().trim().toLowerCase();
            if (!seenFieldNames.add(normalizedName)) {
                throw new BadRequestException("error.template.field.name.duplicate", field.getFieldName());
            }

            if (fileType == FileType.CSV && Boolean.FALSE.equals(hasHeader) && field.getPositionIndex() == null) {
                throw new BadRequestException("error.template.csv.position.index");
            }

            if (field.getDataType() == FieldDataType.DATE || field.getDataType() == FieldDataType.DATETIME) {
                if (!StringUtils.hasText(field.getDateFormat())) {
                    throw new BadRequestException("error.template.field.date.format.required", field.getFieldName());
                }
                validateDatePattern(field.getFieldName(), field.getDateFormat());
            }

            validateRules(field);
        }
        log.debug("Field validation passed for {} field(s)", fields.size());
    }

    private void validateRules(CreateIngestionTemplateRequest.FieldRequest field) {
        if (field.getRules() == null) return;

        for (CreateIngestionTemplateRequest.RuleRequest rule : field.getRules()) {
            TemplateRuleType ruleType = rule.getRuleType();
            String ruleValue = rule.getRuleValue();

            switch (ruleType) {
                case REGEX -> {
                    if (!StringUtils.hasText(ruleValue)) {
                        throw new BadRequestException("error.template.rule.value.required", field.getFieldName(), ruleType);
                    }
                    try {
                        Pattern.compile(ruleValue);
                    } catch (PatternSyntaxException e) {
                        throw new BadRequestException("error.template.rule.regex.invalid", field.getFieldName(), ruleValue);
                    }
                }
                case MIN, MAX -> {
                    if (!StringUtils.hasText(ruleValue) || !isNumeric(ruleValue)) {
                        throw new BadRequestException("error.template.rule.value.numeric", field.getFieldName(), ruleType);
                    }
                }
                case ENUM -> {
                    if (!StringUtils.hasText(ruleValue) || allBlank(ruleValue.split(","))) {
                        throw new BadRequestException("error.template.rule.enum.empty", field.getFieldName());
                    }
                }
                case DATE_FORMAT -> {
                    if (!StringUtils.hasText(ruleValue)) {
                        throw new BadRequestException("error.template.rule.value.required", field.getFieldName(), ruleType);
                    }
                    validateDatePattern(field.getFieldName(), ruleValue);
                }
                case NOT_NULL -> {
                    // no ruleValue expected
                }
            }
        }
    }

    private void validateDatePattern(String fieldName, String pattern) {
        try {
            DateTimeFormatter.ofPattern(pattern);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("error.template.field.date.format.invalid", fieldName, pattern);
        }
    }

    private boolean isNumeric(String value) {
        try {
            new BigDecimal(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean allBlank(String[] values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) return false;
        }
        return true;
    }
}
