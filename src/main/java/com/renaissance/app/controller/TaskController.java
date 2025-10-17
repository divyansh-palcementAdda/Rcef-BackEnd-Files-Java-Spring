package com.renaissance.app.controller;

import com.renaissance.app.exception.BadRequestException;
import com.renaissance.app.exception.ResourcesNotFoundException;
import com.renaissance.app.model.TaskStatus;
import com.renaissance.app.payload.TaskDTO;
import com.renaissance.app.payload.TaskPayload;
import com.renaissance.app.service.interfaces.ITaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
//@CrossOrigin(origins = "*", allowedHeaders = "*")
public class TaskController {

    private final ITaskService taskService;

    // =========================
    // CREATE TASK
    // =========================
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HOD')")
    public ResponseEntity<Map<String, Object>> createTask(@RequestBody TaskPayload payload) {
        try {
            TaskDTO task = taskService.createTask(payload);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("success", true, "data", task, "message", "Task created successfully"));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (ResourcesNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating task: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    // =========================
    // UPDATE TASK
    // =========================
    @PutMapping("/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN','HOD')")
    public ResponseEntity<Map<String, Object>> updateTask(@PathVariable Long taskId, @RequestBody TaskPayload payload) {
        try {
            TaskDTO task = taskService.updateTask(taskId, payload);
            return ResponseEntity.ok(Map.of("success", true, "data", task, "message", "Task updated successfully"));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (ResourcesNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating task {}: {}", taskId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    // =========================
    // DELETE TASK
    // =========================
    @DeleteMapping("/{taskId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteTask(@PathVariable Long taskId) {
        try {
            taskService.deleteTask(taskId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Task deleted successfully"));
        } catch (ResourcesNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting task {}: {}", taskId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    // =========================
    // GET TASK BY ID
    // =========================
    @GetMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskById(@PathVariable Long taskId) {
        try {
            TaskDTO task = taskService.getTaskById(taskId);
            return ResponseEntity.ok(Map.of("success", true, "data", task));
        } catch (ResourcesNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching task {}: {}", taskId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    // =========================
    // GET TASKS BY USER
    // =========================
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getTasksByUser(@PathVariable Long userId) {
        try {
            List<TaskDTO> tasks = taskService.getTasksByUser(userId);
            return ResponseEntity.ok(Map.of("success", true, "data", tasks));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching tasks for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    // =========================
    // GET TASKS BY STATUS
    // =========================
    @GetMapping("/status/{status}")
    public ResponseEntity<Map<String, Object>> getTasksByStatus(@PathVariable TaskStatus status) {
        try {
            List<TaskDTO> tasks = taskService.getAllTasksByStatus(status);
            return ResponseEntity.ok(Map.of("success", true, "data", tasks));
        } catch (Exception e) {
            log.error("Error fetching tasks by status {}: {}", status, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    // =========================
    // GET ALL TASKS
    // =========================
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllTasks() {
        try {
            List<TaskDTO> tasks = taskService.getAllTasks();
            return ResponseEntity.ok(Map.of("success", true, "data", tasks));
        } catch (Exception e) {
            log.error("Error fetching all tasks: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    // =========================
    // APPROVE TASK
    // =========================
    @PutMapping("/{taskId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','HOD')")
    public ResponseEntity<Map<String, Object>> approveTask(@PathVariable Long taskId) {
        try {
            TaskDTO task = taskService.approveTask(taskId);
            return ResponseEntity.ok(Map.of("success", true, "data", task, "message", "Task approved successfully"));
        } catch (ResourcesNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error approving task {}: {}", taskId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    // =========================
    // REJECT TASK
    // =========================
    @PutMapping("/{taskId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','HOD')")
    public ResponseEntity<Map<String, Object>> rejectTask(@PathVariable Long taskId,
                                                          @RequestParam(required = false) String reason) {
        try {
            TaskDTO task = taskService.rejectTask(taskId, reason);
            return ResponseEntity.ok(Map.of("success", true, "data", task, "message", "Task rejected successfully"));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (ResourcesNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error rejecting task {}: {}", taskId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }
}