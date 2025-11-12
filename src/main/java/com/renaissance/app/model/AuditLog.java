package com.renaissance.app.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "audit_logs",
    indexes = {
        @Index(name = "idx_audit_user", columnList = "user_id"),
        @Index(name = "idx_audit_entity", columnList = "entity, entityId"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp DESC")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    @NotNull(message = "Audit log must be associated with a user")
    private User user;

    @NotBlank(message = "Action must not be empty")
    @Size(min = 3, max = 100)
    @Pattern(regexp = "^[A-Za-z0-9 ._-]+$", message = "Invalid action format")
    @Column(nullable = false, length = 100)
    private String action;

    @NotBlank(message = "Entity name must not be empty")
    @Size(min = 2, max = 50)
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Invalid entity name")
    @Column(nullable = false, length = 50)
    private String entity;

    @NotNull(message = "Entity ID must be provided")
    @Positive(message = "Entity ID must be positive")
    @Column(nullable = false)
    private Long entityId;

    @NotNull
    @PastOrPresent
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // Optional: extra details
    @Column(columnDefinition = "TEXT")
    private String details;
}