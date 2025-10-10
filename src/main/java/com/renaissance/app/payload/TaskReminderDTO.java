package com.renaissance.app.payload;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskReminderDTO {
	private Long reminderId;
	private Long taskId;
	private LocalDateTime reminderDate;
	private boolean sent;
}
