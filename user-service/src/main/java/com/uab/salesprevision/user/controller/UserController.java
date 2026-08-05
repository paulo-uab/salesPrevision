package com.uab.salesprevision.user.controller;

import com.uab.salesprevision.user.dto.CreateUserRequest;
import com.uab.salesprevision.user.dto.UpdateUserActiveStatusRequest;
import com.uab.salesprevision.user.dto.UpdateUserRequest;
import com.uab.salesprevision.user.dto.UserResponse;
import com.uab.salesprevision.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        log.debug("POST /api/users - username='{}', companyId={}", request.getUsername(), request.getCompanyId());
        UserResponse response = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> findAll() {
        log.debug("GET /api/users");
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(@PathVariable Long id) {
        log.debug("GET /api/users/{}", id);
        return ResponseEntity.ok(userService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        log.debug("PUT /api/users/{} - companyId={}", id, request.getCompanyId());
        return ResponseEntity.ok(userService.update(id, request));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<UserResponse> updateActiveStatus(@PathVariable Long id, @Valid @RequestBody UpdateUserActiveStatusRequest request) {
        log.debug("PATCH /api/users/{}/active - active={}", id, request.getActive());
        return ResponseEntity.ok(userService.updateActiveStatus(id, request.getActive()));
    }
}
