package com.renaissance.app.payload;

import java.time.LocalDateTime;
import java.util.Set;

import com.renaissance.app.model.TaskStatus;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskPayload {
    @NotBlank
    private String title;

    private String description;

    @Future
    private LocalDateTime dueDate;

    private LocalDateTime startDate;

    @NotNull
    private TaskStatus status;

    private Boolean requiresApproval = false;

    // Single assignment
    private Long assignedToId;
    private Long departmentId;

    // Bulk assignment
    private Set<Long> assignedToIds;
    private Set<Long> departmentIds;
}