package com.renaissance.app.service.interfaces;

import java.util.List;

import com.renaissance.app.model.TaskStatus;
import com.renaissance.app.payload.TaskDTO;
import com.renaissance.app.payload.TaskPayload;

public interface ITaskService {

    /**
     * Create a new task.
     * The current authenticated user will be set as 'createdBy'.
     */
    TaskDTO createTask(TaskPayload payload);

    /**
     * Update an existing task.
     * Only ADMIN or HOD can modify tasks.
     */
    TaskDTO updateTask(Long taskId, TaskPayload payload);

    /**
     * Delete a task (ADMIN only).
     */
    void deleteTask(Long taskId);

    /**
     * Get a single task by its ID.
     */
    TaskDTO getTaskById(Long taskId);

    /**
     * Get all tasks assigned to a specific user.
     */
    List<TaskDTO> getTasksByUser(Long userId);

    /**
     * Get all tasks belonging to a specific department.
     */
    List<TaskDTO> getTasksByDepartment(Long departmentId);

    /**
     * Get all tasks in the system (ADMIN only).
     */
    List<TaskDTO> getAllTasks();

    /**
     * Get tasks filtered by a specific status (e.g. PENDING, CLOSED, etc.).
     */
    List<TaskDTO> getAllTasksByStatus(TaskStatus status);

    /**
     * Approve a task.
     * Only ADMIN or HOD can approve tasks.
     */
    TaskDTO approveTask(Long taskId);

    /**
     * Reject a task.
     * Only ADMIN or HOD can reject tasks, optionally providing a reason.
     */
    TaskDTO rejectTask(Long taskId, String reason);
}
