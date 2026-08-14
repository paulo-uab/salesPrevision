package com.uab.salesprevision.user.auth;

import com.uab.salesprevision.user.model.User;
import com.uab.salesprevision.user.repository.UserRepository;
import com.uab.salesprevision.user.security.JwtProperties;
import com.uab.salesprevision.user.security.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Issues the JWTs that authenticate every other request in the system.")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;

    @PostMapping("/login")
    @Operation(summary = "Log in", description = "The only public endpoint in the whole system (besides JWKS and health/docs). Returns 401 on a wrong username/password or an inactive account.")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        Optional<User> user = userRepository.findByUsernameAndActiveTrue(request.username())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()));

        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<String> roles = user.get().getRoles().stream().map(Enum::name).toList();
        String token = jwtTokenService.generateToken(user.get().getUsername(), roles, user.get().getCompany().getId());
        return ResponseEntity.ok(new LoginResponse(token, jwtProperties.getExpirationMs()));
    }
}
