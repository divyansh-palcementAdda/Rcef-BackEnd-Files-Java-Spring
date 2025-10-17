package com.renaissance.app.payload;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.renaissance.app.model.TaskStatus;
import lombok.*;

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

    // ✅ Multi-user and department support
    private List<Long> assignedToIds;
    private List<String> assignedToNames;

    private List<Long> departmentIds;
    private List<String> departmentNames;

    private boolean requiresApproval;
    private boolean approved;
    private LocalDateTime rfcCompletedAt;

    private List<TaskProofDTO> proofs;
    private List<TaskRequestDTO> requests;
}