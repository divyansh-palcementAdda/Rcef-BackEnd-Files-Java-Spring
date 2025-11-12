package com.renaissance.app.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.renaissance.app.exception.BadRequestException;
import com.renaissance.app.exception.ResourcesNotFoundException;
import com.renaissance.app.exception.UnauthorizedException;
import com.renaissance.app.model.Task;
import com.renaissance.app.model.TaskStatus;
import com.renaissance.app.payload.ApiResult;
import com.renaissance.app.payload.TaskDTO;
import com.renaissance.app.payload.TaskPayload;
import com.renaissance.app.service.impl.JwtService;
import com.renaissance.app.service.interfaces.ITaskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;  // ← CORRECT ANNOTATION
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
public class TaskController {

    private final ITaskService taskService;
    private final JwtService jwtService;

    // ==============================================================
    // CREATE
    // ==============================================================
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HOD')")
    @Operation(summary = "Create a new task")
    @ApiResponse(responseCode = "201", description = "Task created")
    @ApiResponse(responseCode = "400", description = "Bad request")
    @ApiResponse(responseCode = "404", description = "Related resource not found")
    public ResponseEntity<ApiResult<TaskDTO>> createTask(@RequestBody TaskPayload payload) {
        try {
            TaskDTO task = taskService.createTask(payload);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResult.ok(task, "Task created successfully"));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResult.error(e.getMessage(), HttpStatus.BAD_REQUEST));
        } catch (ResourcesNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResult.error(e.getMessage(), HttpStatus.NOT_FOUND));
        } catch (Exception e) {
            log.error("Error creating task", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResult.error("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }
    
    @PatchMapping("/{taskId}/start")
    @PreAuthorize("hasAnyRole('ADMIN','HOD','TEACHER')")
    public ResponseEntity<ApiResult<TaskDTO>> startTask(@PathVariable Long taskId) {
    	TaskDTO task = null;
		try {
			task = taskService.startTask(taskId, jwtService.getUserIdFromAuthentication());
		} catch (ResourcesNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadRequestException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnauthorizedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return ResponseEntity.ok(ApiResult.ok(task, "Task started"));
    }

    // ==============================================================
    // GET BY DEPARTMENT
    // ==============================================================
    @GetMapping("/department/{deptId}")
    @Operation(summary = "Get tasks by department",
               description = "Returns all tasks of users in the given department, sorted newest first.")
    @ApiResponse(responseCode = "200", description = "Tasks retrieved")
    @ApiResponse(responseCode = "400", description = "Invalid department ID")
    public ResponseEntity<ApiResult<List<TaskDTO>>> getTasksByDepartment(
            @PathVariable @NotNull(message = "Department ID is required")
            @Positive(message = "Department ID must be positive") Long deptId) {

        List<TaskDTO> tasks = null;
		try {
			tasks = taskService.getTasksByDepartment(deptId);
		} catch (BadRequestException e) {
			e.printStackTrace();
		}
        return ResponseEntity.ok(ApiResult.ok(tasks,
                "Tasks fetched successfully for department ID: " + deptId));
    }

    // ==============================================================
    // UPDATE
    // ==============================================================
    @PutMapping("/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN','HOD')")
    @Operation(summary = "Update a task")
    @ApiResponse(responseCode = "200", description = "Task updated")
    @ApiResponse(responseCode = "400", description = "Bad request")
    @ApiResponse(responseCode = "404", description = "Task not found")
    public ResponseEntity<ApiResult<TaskDTO>> updateTask(
            @PathVariable Long taskId,
            @RequestBody TaskPayload payload) {
        try {
            TaskDTO task = taskService.updateTask(taskId, payload);
            return ResponseEntity.ok(ApiResult.ok(task, "Task updated successfully"));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResult.error(e.getMessage(), HttpStatus.BAD_REQUEST));
        } catch (ResourcesNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResult.error(e.getMessage(), HttpStatus.NOT_FOUND));
        } catch (Exception e) {
            log.error("Error updating task {}", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResult.error("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    // ==============================================================
    // DELETE
    // ==============================================================
    @DeleteMapping("/{taskId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a task")
    @ApiResponse(responseCode = "200", description = "Task deleted")
    @ApiResponse(responseCode = "404", description = "Task not found")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public ResponseEntity<ApiResult<Void>> deleteTask(@PathVariable Long taskId) {
        try {
            taskService.deleteTask(taskId);
            return ResponseEntity.ok(ApiResult.ok(null, "Task deleted successfully"));
        } catch (ResourcesNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResult.error(e.getMessage(), HttpStatus.NOT_FOUND));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResult.error(e.getMessage(), HttpStatus.BAD_REQUEST));
        } catch (Exception e) {
            log.error("Error deleting task {}", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResult.error("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    // ==============================================================
    // GET BY ID
    // ==============================================================
    @GetMapping("/{taskId}")
    @Operation(summary = "Get task by ID")
    @ApiResponse(responseCode = "200", description = "Task found")
    @ApiResponse(responseCode = "404", description = "Task not found")
    @ApiResponse(responseCode = "400", description = "Invalid ID")
    public ResponseEntity<ApiResult<TaskDTO>> getTaskById(@PathVariable Long taskId) {
        try {
            TaskDTO task = taskService.getTaskById(taskId);
            return ResponseEntity.ok(ApiResult.ok(task));
        } catch (ResourcesNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResult.error(e.getMessage(), HttpStatus.NOT_FOUND));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResult.error(e.getMessage(), HttpStatus.BAD_REQUEST));
        } catch (Exception e) {
            log.error("Error fetching task {}", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResult.error("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    // ==============================================================
    // GET BY USER
    // ==============================================================
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get tasks assigned to a user")
    @ApiResponse(responseCode = "200", description = "Tasks retrieved")
    @ApiResponse(responseCode = "400", description = "Invalid user ID")
    public ResponseEntity<ApiResult<List<TaskDTO>>> getTasksByUser(@PathVariable Long userId) {
        try {
            List<TaskDTO> tasks = taskService.getTasksByUser(userId);
            return ResponseEntity.ok(ApiResult.ok(tasks));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResult.error(e.getMessage(), HttpStatus.BAD_REQUEST));
        } catch (Exception e) {
            log.error("Error fetching tasks for user {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResult.error("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    // ==============================================================
    // GET BY STATUS
    // ==============================================================
    @GetMapping("/status/{status}")
    @Operation(summary = "Get tasks by status")
    @ApiResponse(responseCode = "200", description = "Tasks retrieved")
    @ApiResponse(responseCode = "400", description = "Invalid status")
    public ResponseEntity<ApiResult<List<TaskDTO>>> getTasksByStatus(@PathVariable TaskStatus status) {
        try {
            List<TaskDTO> tasks = taskService.getAllTasksByStatus(status);
            return ResponseEntity.ok(ApiResult.ok(tasks));
        } catch (Exception e) {
            log.error("Error fetching tasks by status {}", status, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResult.error("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    // ==============================================================
    // GET ALL
    // ==============================================================
    @GetMapping
    @Operation(summary = "Get all tasks")
    @ApiResponse(responseCode = "200", description = "All tasks retrieved")
    public ResponseEntity<ApiResult<List<TaskDTO>>> getAllTasks() {
        try {
            List<TaskDTO> tasks = taskService.getAllTasks();
            return ResponseEntity.ok(ApiResult.ok(tasks));
        } catch (Exception e) {
            log.error("Error fetching all tasks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResult.error("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }
    
    
    @GetMapping("/approval")
    @Operation(summary = "Get all tasks which requries approval")
    @ApiResponse(responseCode = "200", description = "All tasks retrieved")
    public ResponseEntity<ApiResult<List<TaskDTO>>> getAllTasksWhichRequriesApproval() {
        try {
            List<TaskDTO> tasks = taskService.getAllTasksWhichRequriesApproval();
            System.err.println(tasks);
            return ResponseEntity.ok(ApiResult.ok(tasks));
        } catch (Exception e) {
            log.error("Error fetching all tasks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResult.error("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    // ==============================================================
    // APPROVE
    // ==============================================================
    @PutMapping("/{taskId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','HOD')")
    @Operation(summary = "Approve a task")
    @ApiResponse(responseCode = "200", description = "Task approved")
    @ApiResponse(responseCode = "404", description = "Task not found")
    public ResponseEntity<ApiResult<TaskDTO>> approveTask(@PathVariable Long taskId) {
        try {
            TaskDTO task = taskService.approveTask(taskId);
            return ResponseEntity.ok(ApiResult.ok(task, "Task approved successfully"));
        } catch (ResourcesNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResult.error(e.getMessage(), HttpStatus.NOT_FOUND));
        } catch (Exception e) {
            log.error("Error approving task {}", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResult.error("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    // ==============================================================
    // REJECT
    // ==============================================================
    @PutMapping("/{taskId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','HOD')")
    @Operation(summary = "Reject a task")
    @ApiResponse(responseCode = "200", description = "Task rejected")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "404", description = "Task not found")
    public ResponseEntity<ApiResult<TaskDTO>> rejectTask(
            @PathVariable Long taskId,
            @RequestParam(required = false) String reason) {
        try {
            TaskDTO task = taskService.rejectTask(taskId, reason);
            return ResponseEntity.ok(ApiResult.ok(task, "Task rejected successfully"));
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResult.error(e.getMessage(), HttpStatus.BAD_REQUEST));
        } catch (ResourcesNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResult.error(e.getMessage(), HttpStatus.NOT_FOUND));
        } catch (Exception e) {
            log.error("Error rejecting task {}", taskId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResult.error("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }
}