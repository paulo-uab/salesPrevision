package com.uab.salesprevision.user.config;

import com.uab.core.enums.ServiceRole;
import com.uab.salesprevision.user.model.Company;
import com.uab.salesprevision.user.model.User;
import com.uab.salesprevision.user.repository.CompanyRepository;
import com.uab.salesprevision.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

/**
 * Seeds the dev accounts on startup, only if they don't already exist —
 * safe to run against a database that persists across restarts, unlike a
 * data.sql insert (which would either fail on the unique constraint or
 * require dropping/recreating the schema every time). Also lets us hash
 * passwords with the real PasswordEncoder instead of pasting precomputed
 * BCrypt hashes into a script.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final String DEFAULT_COMPANY = "Default Company";

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        Company company = companyRepository.findAll().stream()
                .filter(c -> DEFAULT_COMPANY.equals(c.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Company created = companyRepository.save(Company.builder().name(DEFAULT_COMPANY).build());
                    log.info("Seeded company: id={}, name='{}'", created.getId(), created.getName());
                    return created;
                });

        seedUserIfMissing("admin", "admin123", company, EnumSet.allOf(ServiceRole.class));
        seedUserIfMissing("user", "user123", company,
                Set.of(ServiceRole.TEMPLATE_READ, ServiceRole.INGESTION_READ, ServiceRole.PIPELINE_READ, ServiceRole.BATCH_READ));
        seedUserIfMissing("internal-service", "internal-service123", company,
                Set.of(ServiceRole.INGESTION_READ, ServiceRole.PIPELINE_READ));
    }

    private void seedUserIfMissing(String username, String rawPassword, Company company, Set<ServiceRole> roles) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .company(company)
                .roles(roles)
                .active(true)
                .build();
        userRepository.save(user);
        log.info("Seeded user: username='{}', roles={}", username, roles);
    }
}
