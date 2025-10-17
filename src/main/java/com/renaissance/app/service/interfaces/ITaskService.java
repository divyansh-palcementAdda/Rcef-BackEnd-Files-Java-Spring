package com.renaissance.app.service.interfaces;

import java.util.List;

import com.renaissance.app.exception.BadRequestException;
import com.renaissance.app.exception.ResourcesNotFoundException;
import com.renaissance.app.model.TaskStatus;
import com.renaissance.app.payload.TaskDTO;
import com.renaissance.app.payload.TaskPayload;

public interface ITaskService {

    /**
     * Create a new task.
     * The current authenticated user will be set as 'createdBy'.
     * @throws ResourcesNotFoundException 
     * @throws BadRequestException 
     */
    TaskDTO createTask(TaskPayload payload) throws ResourcesNotFoundException, BadRequestException;

    /**
     * Update an existing task.
     * Only ADMIN or HOD can modify tasks.
     * @throws BadRequestException 
     * @throws ResourcesNotFoundException 
     */
    TaskDTO updateTask(Long taskId, TaskPayload payload) throws BadRequestException, ResourcesNotFoundException;

    /**
     * Delete a task (ADMIN only).
     * @throws ResourcesNotFoundException 
     * @throws BadRequestException 
     */
    void deleteTask(Long taskId) throws ResourcesNotFoundException, BadRequestException;

    /**
     * Get a single task by its ID.
     * @throws BadRequestException 
     * @throws ResourcesNotFoundException 
     */
    TaskDTO getTaskById(Long taskId) throws BadRequestException, ResourcesNotFoundException;

    /**
     * Get all tasks assigned to a specific user.
     * @throws BadRequestException 
     */
    List<TaskDTO> getTasksByUser(Long userId) throws BadRequestException;

    /**
     * Get all tasks belonging to a specific department.
     * @throws BadRequestException 
     */
    List<TaskDTO> getTasksByDepartment(Long departmentId) throws BadRequestException;

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
     * @throws ResourcesNotFoundException 
     */
    TaskDTO approveTask(Long taskId) throws ResourcesNotFoundException;

    /**
     * Reject a task.
     * Only ADMIN or HOD can reject tasks, optionally providing a reason.
     * @throws BadRequestException 
     * @throws ResourcesNotFoundException 
     */
    TaskDTO rejectTask(Long taskId, String reason) throws BadRequestException, ResourcesNotFoundException;
}