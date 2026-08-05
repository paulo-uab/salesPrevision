package com.uab.salesprevision.user.service;

import com.uab.core.exception.BadRequestException;
import com.uab.core.exception.ResourceNotFoundException;
import com.uab.salesprevision.user.dto.CreateUserRequest;
import com.uab.salesprevision.user.dto.UpdateUserRequest;
import com.uab.salesprevision.user.dto.UserResponse;
import com.uab.salesprevision.user.model.Company;
import com.uab.salesprevision.user.model.User;
import com.uab.salesprevision.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CompanyService companyService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String username = request.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            log.warn("Duplicate username: '{}'", username);
            throw new BadRequestException("error.user.username.duplicate", username);
        }
        Company company = companyService.getEntity(request.getCompanyId());

        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .company(company)
                .roles(request.getRoles())
                .build();

        User saved = userRepository.save(user);
        log.info("User created: id={}, username='{}', companyId={}", saved.getId(), saved.getUsername(), company.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = getEntity(id);
        Company company = companyService.getEntity(request.getCompanyId());
        user.setCompany(company);
        user.setRoles(request.getRoles());
        User saved = userRepository.save(user);
        log.info("User updated: id={}, companyId={}, roles={}", saved.getId(), company.getId(), saved.getRoles());
        return toResponse(saved);
    }

    @Transactional
    public UserResponse updateActiveStatus(Long id, boolean active) {
        User user = getEntity(id);
        user.setActive(active);
        User saved = userRepository.save(user);
        log.info("User id={} active status set to {}", id, active);
        return toResponse(saved);
    }

    private User getEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found: id={}", id);
                    return new ResourceNotFoundException("error.user.not.found", id);
                });
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .companyId(user.getCompany().getId())
                .companyName(user.getCompany().getName())
                .roles(user.getRoles())
                .active(user.getActive())
                .build();
    }
}
