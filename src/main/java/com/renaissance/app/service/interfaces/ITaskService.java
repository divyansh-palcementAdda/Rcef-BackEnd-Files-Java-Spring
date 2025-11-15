package com.renaissance.app.service.interfaces;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.renaissance.app.exception.BadRequestException;
import com.renaissance.app.exception.ResourcesNotFoundException;
import com.renaissance.app.exception.UnauthorizedException;
import com.renaissance.app.model.TaskStatus;
import com.renaissance.app.payload.TaskDTO;
import com.renaissance.app.payload.TaskPayload;

public interface ITaskService {

    /**
     * Create a new task.
     * The current authenticated user will be set as 'createdBy'.
     * @throws ResourcesNotFoundException 
     * @throws BadRequestException 
     * @throws UnauthorizedException 
     */
    TaskDTO createTask(TaskPayload payload) throws ResourcesNotFoundException, BadRequestException, UnauthorizedException;

    /**
     * Update an existing task.
     * Only ADMIN or HOD can modify tasks.
     * @throws BadRequestException 
     * @throws ResourcesNotFoundException 
     * @throws UnauthorizedException 
     */
    TaskDTO updateTask(Long taskId, TaskPayload payload) throws BadRequestException, ResourcesNotFoundException, UnauthorizedException;

    /**
     * Delete a task (ADMIN only).
     * @throws ResourcesNotFoundException 
     * @throws BadRequestException 
     * @throws UnauthorizedException 
     */
    void deleteTask(Long taskId) throws ResourcesNotFoundException, BadRequestException, UnauthorizedException;

    /**
     * Get a single task by its ID.
     * @throws BadRequestException 
     * @throws ResourcesNotFoundException 
     */
    TaskDTO getTaskById(Long taskId) throws BadRequestException, ResourcesNotFoundException;

    /**
     * Get all tasks assigned to a specific user.
     * @throws BadRequestException 
     * @throws ResourcesNotFoundException 
     */
    List<TaskDTO> getTasksByUser(Long userId) throws BadRequestException, ResourcesNotFoundException;

    /**
     * Get all tasks belonging to a specific department.
     * @throws BadRequestException 
     * @throws ResourcesNotFoundException 
     */
    List<TaskDTO> getTasksByDepartment(Long departmentId) throws BadRequestException, ResourcesNotFoundException;

    /**
     * Get all tasks in the system (ADMIN only).
     */
    List<TaskDTO> getAllTasks();

    
    /**
     * Get all tasks in the system (ADMIN only).
     */
//    List<TaskDTO> getAllTasksWhichRequiresApproval();
    List<TaskDTO> getAllTasksWhichRequriesApproval();
    
    /**
     * Get tasks filtered by a specific status (e.g. PENDING, CLOSED, etc.).
     */
    List<TaskDTO> getAllTasksByStatus(TaskStatus status);

    /**
     * Approve a task.
     * Only ADMIN or HOD can approve tasks.
     * @throws ResourcesNotFoundException 
     * @throws BadRequestException 
     * @throws UnauthorizedException 
     */
    TaskDTO approveTask(Long taskId) throws ResourcesNotFoundException, BadRequestException, UnauthorizedException;

    /**
     * Reject a task.
     * Only ADMIN or HOD can reject tasks, optionally providing a reason.
     * @throws BadRequestException 
     * @throws ResourcesNotFoundException 
     * @throws UnauthorizedException 
     */
    TaskDTO rejectTask(Long taskId, String reason) throws BadRequestException, ResourcesNotFoundException, UnauthorizedException;

	TaskDTO closeTask(Long taskId) throws ResourcesNotFoundException, BadRequestException, UnauthorizedException;

	TaskDTO approveExtension(Long taskId) throws ResourcesNotFoundException, BadRequestException, UnauthorizedException;

	TaskDTO rejectExtension(Long taskId, String reason) throws ResourcesNotFoundException, BadRequestException, UnauthorizedException;

	TaskDTO requestExtension(Long taskId, LocalDateTime newDueDate, String reason)
			throws ResourcesNotFoundException, BadRequestException;

	TaskDTO startTask(Long taskId, Long userId) throws BadRequestException, UnauthorizedException, ResourcesNotFoundException;

//	TaskDTO changeTaskStatus(Long taskId, TaskStatus newStatus) throws ResourcesNotFoundException, BadRequestException, UnauthorizedException;
//
//	TaskDTO requestExtension(Long taskId, LocalDate newDueDate, String reason)
//			throws ResourcesNotFoundException, BadRequestException, UnauthorizedException;



	
}