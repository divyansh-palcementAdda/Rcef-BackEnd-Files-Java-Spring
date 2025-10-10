package com.renaissance.app.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renaissance.app.mapper.TaskMapper;
import com.renaissance.app.model.Department;
import com.renaissance.app.model.Role;
import com.renaissance.app.model.Task;
import com.renaissance.app.model.TaskStatus;
import com.renaissance.app.model.User;
import com.renaissance.app.payload.TaskDTO;
import com.renaissance.app.payload.TaskPayload;
import com.renaissance.app.repository.DepartmentRepository;
import com.renaissance.app.repository.IUserRepository;
import com.renaissance.app.repository.TaskRepository;
import com.renaissance.app.service.interfaces.ITaskService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskServiceImpl implements ITaskService {

    private final TaskRepository taskRepository;
    private final IUserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final TaskMapper taskMapper;

    // ✅ Helper: Get currently logged-in user from JWT
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            throw new SecurityException("Unauthorized: User not authenticated");
        }
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found in system"));
    }

    // ✅ CREATE TASK
    @Override
    @PreAuthorize("hasAnyRole('ADMIN','HOD')")
    public TaskDTO createTask(TaskPayload payload) {
        User creator = getCurrentUser();

        Department dept = departmentRepository.findById(payload.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Department not found with ID: " + payload.getDepartmentId()));

        User assignedUser = null;
        if (payload.getAssignedToId() != null) {
            assignedUser = userRepository.findById(payload.getAssignedToId())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found"));
        }

        Task task = taskMapper.toEntityFromPayload(payload);
        task.setCreatedBy(creator);
        task.setDepartment(dept);
        task.setAssignedTo(assignedUser);
        task.setCreatedAt(LocalDateTime.now());
        task.setStatus(payload.getStatus() != null ? payload.getStatus() : TaskStatus.PENDING);

        Task saved = taskRepository.save(task);
        return taskMapper.toDto(saved);
    }

    // ✅ UPDATE TASK
    @Override
    @PreAuthorize("hasAnyRole('ADMIN','HOD')")
    public TaskDTO updateTask(Long taskId, TaskPayload payload) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with ID: " + taskId));

        if (payload.getTitle() != null) task.setTitle(payload.getTitle());
        if (payload.getDescription() != null) task.setDescription(payload.getDescription());
        if (payload.getDueDate() != null) task.setDueDate(payload.getDueDate());
        if (payload.getAssignedToId() != null) {
            User assigned = userRepository.findById(payload.getAssignedToId())
                    .orElseThrow(() -> new RuntimeException("Assigned user not found"));
            task.setAssignedTo(assigned);
        }
        if (payload.getStatus() != null) task.setStatus(payload.getStatus());

        task.setUpdatedAt(LocalDateTime.now());
        Task updated = taskRepository.save(task);

        return taskMapper.toDto(updated);
    }

    // ✅ DELETE TASK
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        taskRepository.delete(task);
    }

    // ✅ GET BY ID
    @Override
    @Transactional(readOnly = true)
    public TaskDTO getTaskById(Long taskId) {
        return taskRepository.findById(taskId)
                .map(taskMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Task not found with ID: " + taskId));
    }

    // ✅ GET BY USER
    @Override
    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByUser(Long userId) {
        return taskRepository.findByAssignedTo_UserId(userId)
                .stream()
                .map(taskMapper::toDto)
                .collect(Collectors.toList());
    }

    // ✅ GET BY DEPARTMENT
    @Override
    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByDepartment(Long deptId) {
        return taskRepository.findByDepartment_DepartmentId(deptId)
                .stream()
                .map(taskMapper::toDto)
                .collect(Collectors.toList());
    }

    // ✅ GET ALL
    @Override
    @Transactional(readOnly = true)
    public List<TaskDTO> getAllTasks() {
        return taskRepository.findAll()
                .stream()
                .map(taskMapper::toDto)
                .collect(Collectors.toList());
    }

    // ✅ FILTER BY STATUS
    @Override
    @Transactional(readOnly = true)
    public List<TaskDTO> getAllTasksByStatus(TaskStatus status) {
        List<Task> tasks = (status == null) ? taskRepository.findAll() : taskRepository.findByStatus(status);
        return tasks.stream().map(taskMapper::toDto).collect(Collectors.toList());
    }

    // ✅ APPROVE TASK
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','HOD')")
    public TaskDTO approveTask(Long taskId) {
        User approver = getCurrentUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with ID: " + taskId));

        if (!(approver.getRole() == Role.ADMIN || approver.getRole() == Role.HOD)) {
            throw new SecurityException("Only Admin or HOD can approve tasks");
        }

        task.setApproved(true);
        task.setUpdatedAt(LocalDateTime.now());

        Task updated = taskRepository.save(task);
        return taskMapper.toDto(updated);
    }

    // ✅ REJECT TASK
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','HOD')")
    public TaskDTO rejectTask(Long taskId, String reason) {
        User approver = getCurrentUser();
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with ID: " + taskId));

        if (!(approver.getRole() == Role.ADMIN || approver.getRole() == Role.HOD)) {
            throw new SecurityException("Only Admin or HOD can reject tasks");
        }

        task.setApproved(false);
        task.setUpdatedAt(LocalDateTime.now());
        // (optional) You could log the reason in a bulletin or audit entity

        Task updated = taskRepository.save(task);
        return taskMapper.toDto(updated);
    }
}
