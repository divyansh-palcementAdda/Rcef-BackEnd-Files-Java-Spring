package com.renaissance.app.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.renaissance.app.model.TaskStatus;
import com.renaissance.app.payload.TaskDTO;
import com.renaissance.app.payload.TaskPayload;
import com.renaissance.app.service.interfaces.ITaskService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
//@CrossOrigin(origins = "*", allowedHeaders = "*")
public class TaskController {

	private final ITaskService taskService;

	// ✅ Create
	@PostMapping
	@PreAuthorize("hasAnyRole('ADMIN','HOD')")
	public ResponseEntity<TaskDTO> createTask(@RequestBody TaskPayload payload) {
		return ResponseEntity.ok(taskService.createTask(payload));
	}

	// ✅ Update
	@PutMapping("/{taskId}")
	@PreAuthorize("hasAnyRole('ADMIN','HOD')")
	public ResponseEntity<TaskDTO> updateTask(@PathVariable Long taskId, @RequestBody TaskPayload payload) {
		return ResponseEntity.ok(taskService.updateTask(taskId, payload));
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<?> getTaskByUser(@PathVariable Long userId) {
		List<TaskDTO> tasks=taskService.getTasksByUser(userId);
		System.err.println(tasks);
		return ResponseEntity.ok(tasks);

	}

	// ✅ Delete
	@DeleteMapping("/{taskId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
		taskService.deleteTask(taskId);
		return ResponseEntity.noContent().build();
	}

	// ✅ Get by ID
	@GetMapping("/{taskId}")
	public ResponseEntity<TaskDTO> getTaskById(@PathVariable Long taskId) {
		return ResponseEntity.ok(taskService.getTaskById(taskId));
	}

	// ✅ Get by Status
	@GetMapping("/status/{status}")
	public ResponseEntity<List<TaskDTO>> getTasksByStatus(@PathVariable TaskStatus status) {
		return ResponseEntity.ok(taskService.getAllTasksByStatus(status));
	}

	// ✅ Get all
	@GetMapping
	public ResponseEntity<List<TaskDTO>> getAllTasks() {
		return ResponseEntity.ok(taskService.getAllTasks());
	}

	// ✅ Approve
	@PutMapping("/{taskId}/approve")
	@PreAuthorize("hasAnyRole('ADMIN','HOD')")
	public ResponseEntity<TaskDTO> approveTask(@PathVariable Long taskId) {
		return ResponseEntity.ok(taskService.approveTask(taskId));
	}

	// ✅ Reject
	@PutMapping("/{taskId}/reject")
	@PreAuthorize("hasAnyRole('ADMIN','HOD')")
	public ResponseEntity<TaskDTO> rejectTask(@PathVariable Long taskId,
			@RequestParam(required = false) String reason) {
		return ResponseEntity.ok(taskService.rejectTask(taskId, reason));
	}
}
