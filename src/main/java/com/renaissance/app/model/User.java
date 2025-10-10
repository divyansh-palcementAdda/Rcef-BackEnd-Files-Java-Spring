package com.renaissance.app.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "email", name = "UK_user_email")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    // ====================== USERNAME ======================
    @NotBlank(message = "Username must not be empty")
    @Size(min = 3, max = 80, message = "Username must be between 3 and 30 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", 
             message = "Username can only contain letters, numbers, dots, underscores, and hyphens")
    @Column(nullable = false, length = 80)
    private String username;

    // ====================== PASSWORD ======================
    @NotBlank(message = "Password must not be empty")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,64}$",
        message = "Password must contain at least one digit, one uppercase, one lowercase, one special character, and no spaces"
    )
    @Column(nullable = false, length = 200) // encoded password may be long
    private String password;

    // ====================== EMAIL ======================
    @NotBlank(message = "Email must not be empty")
    @Email(message = "Invalid email format")
    @Pattern(
    	    regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z.]+$",
    	    message = "Invalid email format"
    	)
    @Size(max = 200, message = "Email must not exceed 100 characters")
    @Column(nullable = false, unique = true, length = 200)
    private String email;

    // ====================== FULL NAME ======================
    @NotBlank(message = "Full name must not be empty")
    @Size(min = 2, max = 80, message = "Full name must be between 2 and 50 characters")
    @Pattern(
        regexp = "^[A-Za-z]+([ '-][A-Za-z]+)*$",
        message = "Full name can only contain letters, spaces, hyphens, and apostrophes"
    )
    @Column(nullable = false, length = 80)
    private String fullName;

    // ====================== ROLE ======================
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Role must be specified")
    @Column(nullable = false, length = 20)
    private Role role; // ADMIN, SUB_ADMIN, HOD, TEACHER

    // ====================== DEPARTMENT ======================
    @ManyToOne(optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    @NotNull(message = "Department must be provided")
    private Department department;

    // ====================== STATUS ======================
    @Enumerated(EnumType.STRING)
    @NotNull(message = "User status must be specified")
    @Column(nullable = false, length = 20)
    private UserStatus status; // ACTIVE, INACTIVE

    // ====================== EMAIL VERIFICATION ======================
    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column(length = 200)
    private String verificationToken;

    // ====================== AUDIT FIELDS ======================
    @PastOrPresent(message = "Created date cannot be in the future")
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PastOrPresent(message = "Updated date cannot be in the future")
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
