package com.uab.salesprevision.template.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uab.core.enums.FieldDataType;
import com.uab.core.enums.FileType;
import com.uab.core.exception.ResourceNotFoundException;
import com.uab.salesprevision.template.dto.CreateIngestionTemplateRequest;
import com.uab.salesprevision.template.dto.IngestionTemplateResponse;
import com.uab.salesprevision.template.dto.UpdateActiveStatusRequest;
import com.uab.salesprevision.template.service.IngestionTemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IngestionTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IngestionTemplateService service;

    private static RequestPostProcessor readerToken() {
        return jwt().authorities(new SimpleGrantedAuthority("TEMPLATE_READ"));
    }

    private static RequestPostProcessor editorToken() {
        return jwt().authorities(new SimpleGrantedAuthority("TEMPLATE_EDIT"));
    }

    private CreateIngestionTemplateRequest validCreateRequest() {
        return CreateIngestionTemplateRequest.builder()
                .name("Sales Template")
                .fileType(FileType.CSV)
                .hasHeader(true)
                .fields(List.of(CreateIngestionTemplateRequest.FieldRequest.builder()
                        .fieldName("amount")
                        .dataType(FieldDataType.DECIMAL)
                        .build()))
                .build();
    }

    private IngestionTemplateResponse sampleResponse() {
        return IngestionTemplateResponse.builder().id(1L).name("Sales Template").build();
    }

    @Test
    void create_withoutToken_isRejected() throws Exception {
        mockMvc.perform(post("/v1/api/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_withEditorToken_returnsCreated() throws Exception {
        when(service.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/v1/api/templates")
                        .with(editorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Sales Template"));
    }

    @Test
    void create_withReaderToken_returns403() throws Exception {
        mockMvc.perform(post("/v1/api/templates")
                        .with(readerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withEditorToken_missingName_returns400() throws Exception {
        CreateIngestionTemplateRequest invalid = validCreateRequest();
        invalid.setName(" ");

        mockMvc.perform(post("/v1/api/templates")
                        .with(editorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void findById_notFound_returns404WithMessage() throws Exception {
        when(service.findById(99L)).thenThrow(new ResourceNotFoundException("error.template.not.found", 99L));

        // No Accept-Language header -> resolves to the configured default locale (English).
        mockMvc.perform(get("/v1/api/templates/{id}", 99L).with(readerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Template not found with id 99"));
    }

    @Test
    void findAll_returnsPagedBody() throws Exception {
        when(service.findAll(any(), any())).thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/v1/api/templates").with(readerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Sales Template"));
    }

    @Test
    void updateActiveStatus_withEditorToken_returnsUpdatedTemplate() throws Exception {
        when(service.updateActiveStatus(1L, false))
                .thenReturn(IngestionTemplateResponse.builder().id(1L).name("Sales Template").active(false).build());

        mockMvc.perform(patch("/v1/api/templates/{id}/active", 1L)
                        .with(editorToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateActiveStatusRequest.builder().active(false).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void updateActiveStatus_withReaderToken_returns403() throws Exception {
        mockMvc.perform(patch("/v1/api/templates/{id}/active", 1L)
                        .with(readerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateActiveStatusRequest.builder().active(false).build())))
                .andExpect(status().isForbidden());
    }
}
