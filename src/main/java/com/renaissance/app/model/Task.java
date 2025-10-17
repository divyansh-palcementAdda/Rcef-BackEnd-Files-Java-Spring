package com.renaissance.app.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

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

    private LocalDateTime startDate;

    @NotNull(message = "Due date cannot be null")
    @Future(message = "Due date must be in the future")
    private LocalDateTime dueDate;

    @NotNull(message = "Status cannot be null")
    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    // Creator
    @NotNull(message = "Created by user cannot be null")
    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    // ✅ Multiple assigned users
    @ManyToMany
    @JoinTable(
        name = "task_assigned_users",
        joinColumns = @JoinColumn(name = "task_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> assignedUsers = new HashSet<>();

    // ✅ Multiple departments
    @ManyToMany
    @JoinTable(
        name = "task_departments",
        joinColumns = @JoinColumn(name = "task_id"),
        inverseJoinColumns = @JoinColumn(name = "department_id")
    )
    private Set<Department> departments = new HashSet<>();


    @NotNull(message = "Created at date cannot be null")
    @PastOrPresent(message = "Created at date must be in the past or present")
    private LocalDateTime createdAt;

    @PastOrPresent(message = "Updated at date must be in the past or present")
    private LocalDateTime updatedAt;

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
}