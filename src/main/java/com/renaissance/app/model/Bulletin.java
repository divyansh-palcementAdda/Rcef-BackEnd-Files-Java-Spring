package com.renaissance.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "bulletins")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bulletin {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long bulletinId;

	@NotNull(message = "Task cannot be null")
	@ManyToOne
	@JoinColumn(name = "task_id")
	private Task task;

	@NotBlank(message = "Message cannot be blank")
	@Size(min = 1, max = 1000, message = "Message must be between 1 and 1000 characters")
	private String message;

	@NotNull(message = "Severity cannot be null")
	@Enumerated(EnumType.STRING)
	private Severity severity; 

	@NotNull(message = "Created at date cannot be null")
	@PastOrPresent(message = "Created at date must be in the past or present")
	private LocalDateTime createdAt;
}