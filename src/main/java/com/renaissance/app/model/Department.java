package com.renaissance.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "departments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long departmentId;

	@NotBlank(message = "Name cannot be blank")
	@Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
	@Pattern(regexp = "^[a-zA-Z0-9\\s\\-']+$", message = "Name must contain only letters, numbers, spaces, hyphens, or apostrophes")
	@Column(nullable = false, unique = true, length = 100)
	private String name;

	@Size(max = 500, message = "Description cannot exceed 500 characters")
	private String description;

	@NotNull(message = "Created at date cannot be null")
	@PastOrPresent(message = "Created at date must be in the past or present")
	private LocalDateTime createdAt;

	@OneToMany(mappedBy = "department")
	private List<User> users;

	@OneToMany(mappedBy = "department")
	private List<Task> tasks;
}