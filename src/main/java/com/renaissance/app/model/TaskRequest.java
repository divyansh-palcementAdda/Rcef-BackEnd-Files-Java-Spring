package com.renaissance.app.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "task_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long requestId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    @NotNull(message = "Task must be associated with the request")
    private Task task;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Request type must be provided (CLOSURE or EXTENSION)")
    @Column(nullable = false, length = 20)
    private RequestType requestType;

    @ManyToOne(optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    @NotNull(message = "Requesting user must be specified")
    private User requestedBy;

    @NotNull(message = "Request date must be provided")
    @PastOrPresent(message = "Request date cannot be in the future")
    @Column(nullable = false)
    private LocalDateTime requestDate;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Request status must be specified")
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    @Size(max = 1000, message = "Remarks must not exceed 1000 characters")
    @Column(columnDefinition = "TEXT")
    private String remarks;

    @ManyToOne
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    // ==================== PROOFS ====================
    @OneToMany(mappedBy = "taskRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @Size(min = 1, message = "At least one proof is required for the task request")
    private List<TaskProof> proofs = new ArrayList<>();

}