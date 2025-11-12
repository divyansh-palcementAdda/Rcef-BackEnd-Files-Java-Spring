package com.renaissance.app.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
//    @FutureOrPresent(message = "Due date must be in the future or in th present")
    private LocalDateTime dueDate;

    @NotNull(message = "Status cannot be null")
    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    // Creator
    @NotNull(message = "Created by user cannot be null")
    @ManyToOne
    @JoinColumn(name = "created_by")
    @CreatedBy
    private User createdBy;
    
    private Boolean isActive = true;
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
    @CreatedDate
    private LocalDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "started_by", updatable = false)
    private User startedBy;               // WHO started

    @Column(name = "started_at", updatable = false)
    private LocalDateTime startedAt;      // WHEN

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


 // Inside Task.java
    public List<Long> getAssignedUserIds() {
        return assignedUsers.stream().map(User::getUserId).toList();
    }

    public List<Long> getDepartmentIds() {
        return departments.stream().map(Department::getDepartmentId).toList();
    }

    
}