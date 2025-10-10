package com.renaissance.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

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

    // ====================== TASK ======================
    @ManyToOne(optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    @NotNull(message = "Task must be associated with the request")
    private Task task;

    // ====================== REQUEST TYPE ======================
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Request type must be provided (CLOSURE or EXTENSION)")
    @Column(nullable = false, length = 20)
    private RequestType requestType; // CLOSURE, EXTENSION

    // ====================== REQUESTED BY ======================
    @ManyToOne(optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    @NotNull(message = "Requesting user must be specified")
    private User requestedBy;

    // ====================== REQUEST DATE ======================
    @NotNull(message = "Request date must be provided")
    @PastOrPresent(message = "Request date cannot be in the future")
    @Column(nullable = false)
    private LocalDateTime requestDate;

    // ====================== STATUS ======================
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Request status must be specified")
    @Column(nullable = false, length = 20)
    private RequestStatus status; // PENDING, APPROVED, REJECTED

    // ====================== REMARKS ======================
    @Size(max = 1000, message = "Remarks must not exceed 1000 characters")
    @Column(columnDefinition = "TEXT")
    private String remarks;

    // ====================== APPROVED BY ======================
    @ManyToOne
    @JoinColumn(name = "approved_by")
    private User approvedBy; 
}
