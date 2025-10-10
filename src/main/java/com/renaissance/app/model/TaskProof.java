package com.renaissance.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_proofs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskProof {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long proofId;

	@NotNull(message = "Task cannot be null")
	@ManyToOne
	@JoinColumn(name = "task_id")
	private Task task;

	@NotBlank(message = "File URL cannot be blank")
	@Size(min = 1, max = 500, message = "File URL must be between 1 and 500 characters")
	@Pattern(regexp = "^(http|https)://.*$", message = "File URL must be a valid HTTP/HTTPS URL")
	private String fileUrl;

	@NotBlank(message = "File type cannot be blank")
	@Size(min = 1, max = 50, message = "File type must be between 1 and 50 characters")
	private String fileType;

	@NotNull(message = "Uploaded by user cannot be null")
	@ManyToOne
	@JoinColumn(name = "uploaded_by")
	private User uploadedBy;

	@NotNull(message = "Uploaded at date cannot be null")
	@PastOrPresent(message = "Uploaded at date must be in the past or present")
	private LocalDateTime uploadedAt;
}