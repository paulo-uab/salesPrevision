package com.uab.salesprevision.template.service;

import com.uab.core.enums.FieldDataType;
import com.uab.core.enums.FileType;
import com.uab.core.exception.BadRequestException;
import com.uab.core.exception.ResourceNotFoundException;
import com.uab.salesprevision.template.dto.CreateIngestionTemplateRequest;
import com.uab.salesprevision.template.dto.IngestionTemplateResponse;
import com.uab.salesprevision.template.dto.UpdateIngestionTemplateRequest;
import com.uab.salesprevision.template.model.IngestionTemplate;
import com.uab.salesprevision.template.mappers.TemplateEntitiesMapperImpl;
import com.uab.salesprevision.template.repository.IngestionTemplateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngestionTemplateServiceTest {

    private static final Long COMPANY_ID = 1L;

    @Mock
    private IngestionTemplateRepository repository;

    @Mock
    private IngestionTemplateValidationService ingestionTemplateValidationService;

    private IngestionTemplateService service;

    @BeforeEach
    void setUp() {
        service = new IngestionTemplateService(repository, new TemplateEntitiesMapperImpl(), ingestionTemplateValidationService);

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "test-user")
                .claim("companyId", COMPANY_ID)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private CreateIngestionTemplateRequest.FieldRequest.FieldRequestBuilder validField() {
        return CreateIngestionTemplateRequest.FieldRequest.builder()
                .fieldName("amount")
                .dataType(FieldDataType.DECIMAL);
    }

    private CreateIngestionTemplateRequest.CreateIngestionTemplateRequestBuilder validRequest() {
        return CreateIngestionTemplateRequest.builder()
                .name("Sales Template")
                .fileType(FileType.CSV)
                .hasHeader(true)
                .fields(List.of(validField().build()));
    }

    // ---- create ----

    @Test
    void create_savesAndReturnsResponse() {
        when(repository.findByNameAndCompanyId("Sales Template", COMPANY_ID)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(IngestionTemplate.class))).thenAnswer(invocation -> {
            IngestionTemplate template = invocation.getArgument(0);
            template.setId(1L);
            return template;
        });

        IngestionTemplateResponse response = service.create(validRequest().build());

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Sales Template");
        assertThat(response.getFields()).hasSize(1);
        assertThat(response.getFields().get(0).getFieldName()).isEqualTo("amount");
        verify(repository).saveAndFlush(any(IngestionTemplate.class));
    }

    @Test
    void create_stampsCallersCompanyId() {
        when(repository.findByNameAndCompanyId("Sales Template", COMPANY_ID)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(IngestionTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(validRequest().build());

        ArgumentCaptor<IngestionTemplate> captor = ArgumentCaptor.forClass(IngestionTemplate.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getCompanyId()).isEqualTo(COMPANY_ID);
    }

    @Test
    void create_delegatesFieldValidation_andPropagatesFailureWithoutSaving() {
        when(repository.findByNameAndCompanyId("Sales Template", COMPANY_ID)).thenReturn(Optional.empty());
        doThrow(new BadRequestException("error.template.no.fields"))
                .when(ingestionTemplateValidationService).validateFields(any(), anyBoolean(), anyList());

        CreateIngestionTemplateRequest request = validRequest().build();

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.template.no.fields");

        verify(ingestionTemplateValidationService).validateFields(FileType.CSV, true, request.getFields());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void create_duplicateName_rejectedUpfront() {
        when(repository.findByNameAndCompanyId("Sales Template", COMPANY_ID))
                .thenReturn(Optional.of(IngestionTemplate.builder().id(9L).companyId(COMPANY_ID).name("Sales Template").build()));

        assertThatThrownBy(() -> service.create(validRequest().build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.template.name.duplicate");

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void create_duplicateName_raceConditionAtSave() {
        when(repository.findByNameAndCompanyId("Sales Template", COMPANY_ID)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(IngestionTemplate.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        assertThatThrownBy(() -> service.create(validRequest().build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.template.name.duplicate");
    }

    // ---- update ----

    @Test
    void update_notFound_throws() {
        when(repository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.empty());

        UpdateIngestionTemplateRequest request = UpdateIngestionTemplateRequest.builder()
                .name("X").fileType(FileType.CSV).hasHeader(true)
                .fields(List.of(validField().build()))
                .build();

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("error.template.not.found");
    }

    @Test
    void update_nameTakenByAnotherTemplate_rejected() {
        IngestionTemplate existing = IngestionTemplate.builder().id(1L).companyId(COMPANY_ID).name("Old Name").version(1).build();
        when(repository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.of(existing));
        when(repository.findByNameAndCompanyId("New Name", COMPANY_ID))
                .thenReturn(Optional.of(IngestionTemplate.builder().id(2L).companyId(COMPANY_ID).name("New Name").build()));

        UpdateIngestionTemplateRequest request = UpdateIngestionTemplateRequest.builder()
                .name("New Name").fileType(FileType.CSV).hasHeader(true)
                .fields(List.of(validField().build()))
                .build();

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.template.name.duplicate");
    }

    @Test
    void update_sameNameAsSelf_allowedAndVersionIncremented() {
        IngestionTemplate existing = IngestionTemplate.builder()
                .id(1L).companyId(COMPANY_ID).name("Sales Template").version(1).fields(new ArrayList<>())
                .build();
        when(repository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.of(existing));
        when(repository.findByNameAndCompanyId("Sales Template", COMPANY_ID)).thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(any(IngestionTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateIngestionTemplateRequest request = UpdateIngestionTemplateRequest.builder()
                .name("Sales Template").fileType(FileType.CSV).hasHeader(true)
                .description("updated")
                .fields(List.of(validField().build()))
                .build();

        IngestionTemplateResponse response = service.update(1L, request);

        assertThat(response.getDescription()).isEqualTo("updated");
        assertThat(response.getVersion()).isEqualTo(2);
        assertThat(response.getFields()).hasSize(1);
    }

    @Test
    void update_duplicateNameRaceConditionAtSave_rejected() {
        IngestionTemplate existing = IngestionTemplate.builder()
                .id(1L).companyId(COMPANY_ID).name("Sales Template").version(1).fields(new ArrayList<>())
                .build();
        when(repository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.of(existing));
        when(repository.findByNameAndCompanyId("Sales Template", COMPANY_ID)).thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(any(IngestionTemplate.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        UpdateIngestionTemplateRequest request = UpdateIngestionTemplateRequest.builder()
                .name("Sales Template").fileType(FileType.CSV).hasHeader(true)
                .fields(List.of(validField().build()))
                .build();

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("error.template.name.duplicate");
    }

    // ---- updateActiveStatus ----

    @Test
    void updateActiveStatus_flipsFlag() {
        IngestionTemplate existing = IngestionTemplate.builder().id(1L).companyId(COMPANY_ID).name("T").active(true).build();
        when(repository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.of(existing));
        when(repository.save(any(IngestionTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IngestionTemplateResponse response = service.updateActiveStatus(1L, false);

        assertThat(response.getActive()).isFalse();
    }

    @Test
    void updateActiveStatus_notFound_throws() {
        when(repository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateActiveStatus(1L, false))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("error.template.not.found");
    }

    // ---- findAll ----

    @Test
    void findAll_filtersByActiveWhenProvided() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findByCompanyIdAndActive(COMPANY_ID, true, pageable)).thenReturn(new PageImpl<>(List.of()));

        service.findAll(true, pageable);

        verify(repository).findByCompanyIdAndActive(COMPANY_ID, true, pageable);
        verify(repository, never()).findByCompanyId(any(), any());
    }

    @Test
    void findAll_returnsEverythingWhenActiveNotProvided() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findByCompanyId(COMPANY_ID, pageable)).thenReturn(new PageImpl<>(List.of()));

        service.findAll(null, pageable);

        verify(repository).findByCompanyId(COMPANY_ID, pageable);
        verify(repository, never()).findByCompanyIdAndActive(any(), any(), any());
    }

    // ---- findById ----

    @Test
    void findById_notFound_throws() {
        when(repository.findByIdAndCompanyId(42L, COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("error.template.not.found");
    }
}
