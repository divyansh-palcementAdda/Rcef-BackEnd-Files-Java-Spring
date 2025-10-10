package com.renaissance.app.payload;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.renaissance.app.model.TaskStatus;

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
	private LocalDateTime dueDate;
	private TaskStatus status;

	private Long createdById;
	private String createdByName;

	private Long assignedToId;
	private String assignedToName;

	private Long departmentId;
	private String departmentName;

	private boolean requiresApproval; // NEW
	private boolean approved; // NEW
	private LocalDateTime rfcCompletedAt; // NEW

	private List<TaskProofDTO> proofs;
	private List<TaskRequestDTO> requests;
}