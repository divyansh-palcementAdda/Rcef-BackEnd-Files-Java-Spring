package com.renaissance.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long taskId;

	@NotBlank(message = "Title cannot be blank")
	@Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
	private String title;

	@Size(max = 2000, message = "Description cannot exceed 2000 characters")
	private String description;

	@NotNull(message = "Due date cannot be null")
	@Future(message = "Due date must be in the future")
	private LocalDateTime dueDate;

	@NotNull(message = "Status cannot be null")
	@Enumerated(EnumType.STRING)
	private TaskStatus status;

	@NotNull(message = "Created by user cannot be null")
	@ManyToOne
	@JoinColumn(name = "created_by")
	private User createdBy;

	@ManyToOne
	@JoinColumn(name = "assigned_to")
	private User assignedTo;

	@NotNull(message = "Department cannot be null")
	@ManyToOne
	@JoinColumn(name = "department_id")
	private Department department;

	@NotNull(message = "Created at date cannot be null")
	@PastOrPresent(message = "Created at date must be in the past or present")
	private LocalDateTime createdAt;

	@PastOrPresent(message = "Updated at date must be in the past or present")
	private LocalDateTime updatedAt;

	// NEW fields
	private boolean requiresApproval; 
	private boolean approved; 

	@PastOrPresent(message = "RFC completed at date must be in the past or present")
	private LocalDateTime rfcCompletedAt; 

	@OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
	private List<TaskProof> proofs;

	@OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
	private List<TaskRequest> requests;

	@OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
	private List<Bulletin> bulletins;
	
//	private String remarks;
}