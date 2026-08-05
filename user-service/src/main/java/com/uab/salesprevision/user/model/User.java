package com.uab.salesprevision.user.model;

import com.uab.core.enums.ServiceRole;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    // EAGER: every path that loads a User (login, response DTOs) needs its
    // company right away, and AuthController.login() reads it outside of any
    // transaction — LAZY here throws LazyInitializationException there.
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Builder.Default
    @ElementCollection(targetClass = ServiceRole.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private Set<ServiceRole> roles = new HashSet<>();

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
}
