package com.renaissance.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    // ====================== USER ======================
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "Audit log must be associated with a user")
    private User user;

    // ====================== ACTION ======================
    @NotBlank(message = "Action must not be empty")
    @Size(min = 3, max = 100, message = "Action must be between 3 and 100 characters")
    @Pattern(
        regexp = "^[A-Za-z0-9 ._-]+$",
        message = "Action can only contain letters, numbers, spaces, dots, underscores, and hyphens"
    )
    @Column(nullable = false, length = 100)
    private String action; // e.g., "Created Task"

    // ====================== ENTITY ======================
    @NotBlank(message = "Entity name must not be empty")
    @Size(min = 2, max = 50, message = "Entity name must be between 2 and 50 characters")
    @Pattern(
        regexp = "^[A-Za-z0-9_-]+$",
        message = "Entity name can only contain letters, numbers, underscores, and hyphens"
    )
    @Column(nullable = false, length = 50)
    private String entity; // e.g., "Task"

    // ====================== ENTITY ID ======================
    @NotNull(message = "Entity ID must be provided")
    @Positive(message = "Entity ID must be a positive number")
    @Column(nullable = false)
    private Long entityId; // related entity ID

    // ====================== TIMESTAMP ======================
    @NotNull(message = "Timestamp must be provided")
    @PastOrPresent(message = "Timestamp cannot be in the future")
    @Column(nullable = false)
    private LocalDateTime timestamp;
}
