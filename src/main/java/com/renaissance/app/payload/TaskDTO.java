package com.renaissance.app.payload;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.renaissance.app.model.TaskStatus;
import com.renaissance.app.model.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskDTO {
    private Long taskId;
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime dueDate;
    private TaskStatus status;

    private Long createdById;
    private String createdByName;
    
    private LocalDateTime startedAt;
    private Long startedById;               // WHO started
    private String startedByName;


    // ✅ Multi-user and department support
    private List<Long> assignedToIds;
    private List<String> assignedToNames;

    private List<Long> departmentIds;
    private List<String> departmentNames;

    private boolean requiresApproval;
    private boolean approved;
    private LocalDateTime rfcCompletedAt;

    private List<TaskRequestDTO> requests;
}