package com.uab.salesprevision.template.service;

import com.uab.core.enums.FieldDataType;
import com.uab.core.enums.FileType;
import com.uab.core.enums.TemplateRuleType;
import com.uab.core.exception.BadRequestException;
import com.uab.salesprevision.template.dto.CreateIngestionTemplateRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IngestionTemplateValidationServiceTest {

    private final IngestionTemplateValidationService service = new IngestionTemplateValidationService();

    private CreateIngestionTemplateRequest.FieldRequest.FieldRequestBuilder validField() {
        return CreateIngestionTemplateRequest.FieldRequest.builder()
                .fieldName("amount")
                .dataType(FieldDataType.DECIMAL);
    }

    @Test
    void noFields_rejected() {
        assertThatThrownBy(() -> service.validateFields(FileType.CSV, true, List.of()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.template.no.fields");
    }

    @Test
    void blankFieldName_rejected() {
        assertThatThrownBy(() -> service.validateFields(FileType.CSV, true,
                List.of(validField().fieldName("  ").build())))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.template.field.name.empty");
    }

    @Test
    void duplicateFieldName_rejected() {
        assertThatThrownBy(() -> service.validateFields(FileType.CSV, true, List.of(
                validField().fieldName("amount").build(),
                validField().fieldName("Amount").build())))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.template.field.name.duplicate");
    }

    @Test
    void csvWithoutHeaderMissingPositionIndex_rejected() {
        assertThatThrownBy(() -> service.validateFields(FileType.CSV, false,
                List.of(validField().build())))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.template.csv.position.index");
    }

    @Test
    void csvWithoutHeaderWithPositionIndex_accepted() {
        assertThatCode(() -> service.validateFields(FileType.CSV, false,
                List.of(validField().positionIndex(0).build())))
                .doesNotThrowAnyException();
    }

    @Test
    void dateFieldWithoutDateFormat_rejected() {
        assertThatThrownBy(() -> service.validateFields(FileType.CSV, true,
                List.of(validField().dataType(FieldDataType.DATE).build())))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.template.field.date.format.required");
    }

    @Test
    void dateFieldWithInvalidDateFormat_rejected() {
        assertThatThrownBy(() -> service.validateFields(FileType.CSV, true,
                List.of(validField().dataType(FieldDataType.DATETIME).dateFormat("yyyy-MM-dd'").build())))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.template.field.date.format.invalid");
    }

    @Test
    void dateFieldWithValidDateFormat_accepted() {
        assertThatCode(() -> service.validateFields(FileType.CSV, true,
                List.of(validField().dataType(FieldDataType.DATE).dateFormat("yyyy-MM-dd").build())))
                .doesNotThrowAnyException();
    }

    @Test
    void regexRuleWithInvalidPattern_rejected() {
        assertThatThrownBy(() -> service.validateFields(FileType.CSV, true, List.of(validField()
                .rules(List.of(CreateIngestionTemplateRequest.RuleRequest.builder()
                        .ruleType(TemplateRuleType.REGEX)
                        .ruleValue("[")
                        .build()))
                .build())))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.template.rule.regex.invalid");
    }

    @Test
    void minRuleWithNonNumericValue_rejected() {
        assertThatThrownBy(() -> service.validateFields(FileType.CSV, true, List.of(validField()
                .rules(List.of(CreateIngestionTemplateRequest.RuleRequest.builder()
                        .ruleType(TemplateRuleType.MIN)
                        .ruleValue("not-a-number")
                        .build()))
                .build())))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.template.rule.value.numeric");
    }

    @Test
    void enumRuleWithEmptyValue_rejected() {
        assertThatThrownBy(() -> service.validateFields(FileType.CSV, true, List.of(validField()
                .rules(List.of(CreateIngestionTemplateRequest.RuleRequest.builder()
                        .ruleType(TemplateRuleType.ENUM)
                        .ruleValue(" , , ")
                        .build()))
                .build())))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.template.rule.enum.empty");
    }

    @Test
    void dateFormatRuleWithoutValue_rejected() {
        assertThatThrownBy(() -> service.validateFields(FileType.CSV, true, List.of(validField()
                .rules(List.of(CreateIngestionTemplateRequest.RuleRequest.builder()
                        .ruleType(TemplateRuleType.DATE_FORMAT)
                        .build()))
                .build())))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.template.rule.value.required");
    }

    @Test
    void notNullRuleWithoutValue_accepted() {
        assertThatCode(() -> service.validateFields(FileType.CSV, true, List.of(validField()
                .rules(List.of(CreateIngestionTemplateRequest.RuleRequest.builder()
                        .ruleType(TemplateRuleType.NOT_NULL)
                        .build()))
                .build())))
                .doesNotThrowAnyException();
    }
}
