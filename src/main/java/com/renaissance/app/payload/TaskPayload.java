package com.renaissance.app.payload;

import java.time.LocalDateTime;

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
public class TaskPayload {
	private String title;
	private String description;
	private LocalDateTime dueDate;
	private Long assignedToId;
	private Long departmentId;
	private boolean requiresApproval; // NEW (true if created by HOD)
	private TaskStatus status;
}