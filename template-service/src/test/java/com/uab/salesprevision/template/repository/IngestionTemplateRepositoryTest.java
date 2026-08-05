package com.uab.salesprevision.template.repository;

import com.uab.core.enums.FileType;
import com.uab.salesprevision.template.model.IngestionTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class IngestionTemplateRepositoryTest {

    private static final Long COMPANY_A = 1L;
    private static final Long COMPANY_B = 2L;

    @Autowired
    private IngestionTemplateRepository repository;

    @Test
    void existsByName_and_findByName_areScopedToCompany() {
        repository.save(IngestionTemplate.builder().companyId(COMPANY_A).name("Sales Template").fileType(FileType.CSV).build());

        assertThat(repository.existsByNameAndCompanyId("Sales Template", COMPANY_A)).isTrue();
        assertThat(repository.existsByNameAndCompanyId("Sales Template", COMPANY_B)).isFalse();
        assertThat(repository.existsByNameAndCompanyId("Other", COMPANY_A)).isFalse();
        assertThat(repository.findByNameAndCompanyId("Sales Template", COMPANY_A)).isPresent();
        assertThat(repository.findByNameAndCompanyId("Sales Template", COMPANY_B)).isEmpty();
    }

    @Test
    void sameName_allowedAcrossDifferentCompanies() {
        repository.save(IngestionTemplate.builder().companyId(COMPANY_A).name("Sales Template").fileType(FileType.CSV).build());

        IngestionTemplate saved = repository.save(
                IngestionTemplate.builder().companyId(COMPANY_B).name("Sales Template").fileType(FileType.CSV).build());

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void findByCompanyIdAndActive_filtersCorrectly() {
        repository.save(IngestionTemplate.builder().companyId(COMPANY_A).name("Active One").fileType(FileType.CSV).active(true).build());
        repository.save(IngestionTemplate.builder().companyId(COMPANY_A).name("Inactive One").fileType(FileType.CSV).active(false).build());
        repository.save(IngestionTemplate.builder().companyId(COMPANY_B).name("Other Company Active").fileType(FileType.CSV).active(true).build());

        Page<IngestionTemplate> activeOnly = repository.findByCompanyIdAndActive(COMPANY_A, true, PageRequest.of(0, 10));
        Page<IngestionTemplate> inactiveOnly = repository.findByCompanyIdAndActive(COMPANY_A, false, PageRequest.of(0, 10));

        assertThat(activeOnly.getTotalElements()).isEqualTo(1);
        assertThat(activeOnly.getContent().get(0).getName()).isEqualTo("Active One");
        assertThat(inactiveOnly.getTotalElements()).isEqualTo(1);
        assertThat(inactiveOnly.getContent().get(0).getName()).isEqualTo("Inactive One");
    }

    @Test
    void findByCompanyId_doesNotLeakOtherCompaniesData() {
        repository.save(IngestionTemplate.builder().companyId(COMPANY_A).name("A Template").fileType(FileType.CSV).build());
        repository.save(IngestionTemplate.builder().companyId(COMPANY_B).name("B Template").fileType(FileType.CSV).build());

        Page<IngestionTemplate> companyAResults = repository.findByCompanyId(COMPANY_A, PageRequest.of(0, 10));

        assertThat(companyAResults.getTotalElements()).isEqualTo(1);
        assertThat(companyAResults.getContent().get(0).getName()).isEqualTo("A Template");
    }

    @Test
    void findByIdAndCompanyId_doesNotReturnAnotherCompanysRow() {
        IngestionTemplate saved = repository.save(
                IngestionTemplate.builder().companyId(COMPANY_A).name("A Template").fileType(FileType.CSV).build());

        assertThat(repository.findByIdAndCompanyId(saved.getId(), COMPANY_A)).isPresent();
        assertThat(repository.findByIdAndCompanyId(saved.getId(), COMPANY_B)).isEmpty();
    }

    @Test
    void defaults_applyOnSave() {
        IngestionTemplate saved = repository.save(IngestionTemplate.builder()
                .companyId(COMPANY_A)
                .name("Defaults Template")
                .fileType(FileType.CSV)
                .build());

        assertThat(saved.getActive()).isTrue();
        assertThat(saved.getVersion()).isEqualTo(1);
        assertThat(saved.getHasHeader()).isTrue();
    }
}
