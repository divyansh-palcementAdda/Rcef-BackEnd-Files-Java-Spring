package com.renaissance.app.model;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users", uniqueConstraints = { @UniqueConstraint(columnNames = "email", name = "UK_user_email") })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long userId;

	@NotBlank
	@Size(min = 3, max = 80)
	@Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username can only contain letters, digits, dots, underscores, and hyphens")
	@Column(nullable = false, length = 80)
	private String username;

	@NotBlank
	@Size(min = 8, max = 64)
	@Column(nullable = false, length = 200)
	private String password;

	@NotBlank
	@Email
	@Size(max = 200)
	@Column(nullable = false, unique = true, length = 200)
	private String email;

	@NotBlank
	@Size(min = 2, max = 80)
	@Pattern(regexp = "^[a-zA-Z ]+$", message = "Full name can only contain alphabets and spaces")
	@Column(nullable = false, length = 80)
	private String fullName;

	@Enumerated(EnumType.STRING)
	@NotNull
	@Column(nullable = false, length = 20)
	private Role role;

	@Enumerated(EnumType.STRING)
	@NotNull
	@Column(nullable = false, length = 20)
	private UserStatus status;

	// ✅ Corrected relation: Many-to-Many between User and Department
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "user_departments", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "department_id"))
	private List<Department> departments;

	@Column(nullable = false)
	private boolean emailVerified = false;

	@Column(length = 200)
	private String verificationToken;

	@PastOrPresent
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PastOrPresent
	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}
}
