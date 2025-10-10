package com.renaissance.app.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "task_reminders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskReminder {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long reminderId;

	@NotNull(message = "Task cannot be null")
	@ManyToOne
	@JoinColumn(name = "task_id")
	private Task task;

	@NotNull(message = "Reminder date cannot be null")
	@FutureOrPresent(message = "Reminder date must be in the future or present")
	private LocalDateTime reminderDate; // activation + 2 days before deadline
	private boolean sent;
}