package com.renaissance.app.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(
    name = "users",
    uniqueConstraints = {@UniqueConstraint(columnNames = "email", name = "UK_user_email")}
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @NotBlank
    @Size(min = 3, max = 80)
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$")
    @Column(nullable = false, length = 80)
    private String username;

    @NotBlank
    @Size(min = 8, max = 64)
    @Column(nullable = false, length = 200) // encoded password
    private String password;

    @NotBlank
    @Email
    @Size(max = 200)
    @Column(nullable = false, unique = true, length = 200)
    private String email;

    @NotBlank
    @Size(min = 2, max = 80)
    @Column(nullable = false, length = 80)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column(length = 200)
    private String verificationToken;

    @PastOrPresent
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PastOrPresent
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
