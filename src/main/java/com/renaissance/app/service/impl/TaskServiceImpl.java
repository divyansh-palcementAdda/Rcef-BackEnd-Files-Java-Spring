package com.renaissance.app.service.impl;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renaissance.app.exception.BadRequestException;
import com.renaissance.app.exception.ResourcesNotFoundException;
import com.renaissance.app.exception.UnauthorizedException;
import com.renaissance.app.mapper.TaskMapper;
import com.renaissance.app.model.AuditLog;
import com.renaissance.app.model.Department;
import com.renaissance.app.model.Role;
import com.renaissance.app.model.Task;
import com.renaissance.app.model.TaskStatus;
import com.renaissance.app.model.User;
import com.renaissance.app.payload.NotificationEvent;
import com.renaissance.app.payload.TaskDTO;
import com.renaissance.app.payload.TaskPayload;
import com.renaissance.app.repository.AuditLogRepository;
import com.renaissance.app.repository.DepartmentRepository;
import com.renaissance.app.repository.IUserRepository;
import com.renaissance.app.repository.TaskRepository;
import com.renaissance.app.service.interfaces.ITaskService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TaskServiceImpl implements ITaskService {
	private final TaskRepository taskRepository;
	private final IUserRepository userRepository;
	private final DepartmentRepository departmentRepository;
	private final TaskMapper taskMapper;
	private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
	private final AuditLogRepository auditLogRepository; 

	// ===========================================================
		// GET CURRENT USER
		// ===========================================================
		private User getCurrentUser() throws ResourcesNotFoundException {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
				throw new ResourcesNotFoundException("User not authenticated");
			}
			return userRepository.findByUsername(auth.getName())
					.orElseThrow(() -> new ResourcesNotFoundException("Authenticated user not found"));
		}

	// ===========================================================
	// CREATE TASK
	// ===========================================================
		@Override
		@PreAuthorize("hasAnyRole('ADMIN','HOD')")
		public TaskDTO createTask(TaskPayload payload) throws ResourcesNotFoundException, BadRequestException {
		    validatePayloadMinimal(payload);
		    User creator = getCurrentUser();
		    validateDatesForPayload(payload);

		    Set<Department> departments = resolveDepartments(payload);
		    if (creator.getRole() == Role.HOD) {
		        enforceHodDepartmentConstraint(creator, departments);
		    }

		    Set<User> assignedUsers = resolveAssignedUsers(payload, departments, creator);
		    Task task = taskMapper.toEntityFromPayload(payload);
		    task.setCreatedBy(creator);
		    task.setCreatedAt(LocalDateTime.now());
		    task.setUpdatedAt(LocalDateTime.now());
		    task.setStartDate(Optional.ofNullable(payload.getStartDate()).orElse(LocalDateTime.now()));
		    task.setStatus(Optional.ofNullable(payload.getStatus()).orElse(TaskStatus.PENDING));
		    setApprovalBasedOnRole(task, creator);
		    task.setDepartments(departments);
		    task.setAssignedUsers(assignedUsers);

		    task = taskRepository.save(task);

		    // === AUDIT LOG ===
		    logAudit(creator, "TASK_CREATED", "Task", task.getTaskId(),
		            String.format("Title: %s | Status: %s | Requires Approval: %s",
		                    task.getTitle(), task.getStatus(), task.isRequiresApproval()));

		    // === KAFKA ===
		    publishTaskEvent("TASK_CREATED", task,
		            creator.getRole() == Role.HOD ? "Task created by HOD - pending approval" : "Task created by Admin");

		    return taskMapper.toDto(task);
		}
	// ===========================================================
	// UPDATE TASK
	// ===========================================================
		@Override
		@PreAuthorize("hasAnyRole('ADMIN','HOD')")
		public TaskDTO updateTask(Long taskId, TaskPayload payload) throws ResourcesNotFoundException, BadRequestException {
		    Task task = taskRepository.findById(taskId)
		            .orElseThrow(() -> new ResourcesNotFoundException("Task not found"));
		    User currentUser = getCurrentUser();

		    if (currentUser.getRole() == Role.HOD
		            && !task.getDepartments().stream().anyMatch(d -> currentUser.getDepartments().contains(d))) {
		        throw new BadRequestException("HOD can only update tasks in their department");
		    }

		    updateTaskFields(task, payload);
		    task.setUpdatedAt(LocalDateTime.now());
		    task = taskRepository.save(task);

		    // === AUDIT LOG ===
		    logAudit(currentUser, "TASK_UPDATED", "Task", taskId,
		            payload.getStatus() != null ? "Status → " + payload.getStatus() : "Fields updated");

		    // === KAFKA ===
		    if (payload.getStatus() != null) {
		        publishTaskEvent("TASK_UPDATED", task, "Task status updated to " + payload.getStatus());
		    }

		    return taskMapper.toDto(task);
		}
	// ===========================================================
	// DELETE TASK
	// ===========================================================
		@Override
		@PreAuthorize("hasRole('ADMIN')")
		public void deleteTask(Long taskId) throws ResourcesNotFoundException, BadRequestException {
		    if (taskId == null) throw new BadRequestException("Task ID is required");

		    Task task = taskRepository.findById(taskId)
		            .orElseThrow(() -> new ResourcesNotFoundException("Task not found with ID: " + taskId));

		    if (!task.getStatus().equals(TaskStatus.CLOSED)) {
		        throw new BadRequestException("Task is Still Open. Close it first before deleting.");
		    }

		    task.setIsActive(false);
		    taskRepository.save(task);

		    // === AUDIT LOG ===
		    logAudit(getCurrentUser(), "TASK_DELETED", "Task", taskId,
		            "Title: " + task.getTitle());

		    log.info("Task deleted: id={}, title='{}'", task.getTaskId(), task.getTitle());
		}
	// ===========================================================
	// GETTERS
	// ===========================================================
	@Override
	@Transactional(readOnly = true)
	public TaskDTO getTaskById(Long taskId) throws BadRequestException, ResourcesNotFoundException {
		if (taskId == null)
			throw new BadRequestException("Task ID is required");
		return taskRepository.findById(taskId).map(taskMapper::toDto)
				.orElseThrow(() -> new ResourcesNotFoundException("Task not found with ID: " + taskId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<TaskDTO> getAllTasks() {
		return taskRepository.findAll().stream().sorted(Comparator.comparing(Task::getCreatedAt).reversed())
				.map(taskMapper::toDto).collect(Collectors.toList());
	}
	@Override
	public List<TaskDTO> getAllTasksWhichRequriesApproval() {
	    return taskRepository.findByRequiresApprovalTrueAndApprovedFalse()
	            .stream()
	            .map(taskMapper::toDto)
	            .collect(Collectors.toList());
	}
	@Override
	@Transactional(readOnly = true)
	public List<TaskDTO> getAllTasksByStatus(TaskStatus status) {
		List<Task> tasks = (status == null) ? taskRepository.findAll() : taskRepository.findByStatus(status);
		return tasks.stream().sorted(Comparator.comparing(Task::getCreatedAt).reversed()).map(taskMapper::toDto)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<TaskDTO> getTasksByUser(Long userId) throws BadRequestException {
		if (userId == null)
			throw new BadRequestException("User ID is required");
		return taskRepository.findByAssignedUsers_UserId(userId).stream()
				.sorted(Comparator.comparing(Task::getCreatedAt).reversed()).map(taskMapper::toDto)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<TaskDTO> getTasksByDepartment(Long deptId) throws BadRequestException {
		if (deptId == null)
			throw new BadRequestException("Department ID is required");
		return taskRepository.findByDepartments_DepartmentId(deptId).stream()
				.sorted(Comparator.comparing(Task::getCreatedAt).reversed()).map(taskMapper::toDto)
				.collect(Collectors.toList());
	}

	// ===========================================================
	// APPROVE TASK
	// ===========================================================
	@Override
	@PreAuthorize("hasRole('ADMIN')")
	public TaskDTO approveTask(Long taskId) throws ResourcesNotFoundException, BadRequestException {
	    Task task = taskRepository.findById(taskId)
	            .orElseThrow(() -> new ResourcesNotFoundException("Task not found"));
	    User approver = getCurrentUser();

	    validateApproverRole(approver);
	    if (!task.isRequiresApproval() || task.isApproved()) {
	        throw new BadRequestException("Task does not require approval or already approved");
	    }

	    task.setApproved(true);
	    task.setUpdatedAt(LocalDateTime.now());
	    task = taskRepository.save(task);

	    // === AUDIT LOG ===
	    logAudit(approver, "TASK_APPROVED", "Task", taskId, "HOD task approved");

	    // === KAFKA ===
	    publishTaskEvent("TASK_APPROVED", task, "Task approved by Admin");

	    return taskMapper.toDto(task);
	}

	// ===========================================================
	// REJECT TASK
	// ===========================================================
	@Override
	@PreAuthorize("hasRole('ADMIN')")
	public TaskDTO rejectTask(Long taskId, String reason) throws ResourcesNotFoundException, BadRequestException {
	    Task task = taskRepository.findById(taskId)
	            .orElseThrow(() -> new ResourcesNotFoundException("Task not found"));
	    User approver = getCurrentUser();

	    validateApproverRole(approver);
	    if (!task.isRequiresApproval()) {
	        throw new BadRequestException("Task does not require approval");
	    }

	    task.setApproved(false);
	    task.setStatus(TaskStatus.PENDING);
	    task.setUpdatedAt(LocalDateTime.now());
	    task = taskRepository.save(task);

	    // === AUDIT LOG ===
	    logAudit(approver, "TASK_REJECTED", "Task", taskId,
	            "Reason: " + (reason != null ? reason : "No reason"));

	    // === KAFKA ===
	    publishTaskEvent("TASK_REJECTED", task, "Task rejected: " + (reason != null ? reason : "No reason"));

	    return taskMapper.toDto(task);
	}

	// ===========================================================
	// CLOSE TASK
	// ===========================================================
	@Override
	@PreAuthorize("hasAnyRole('ADMIN')")
	public TaskDTO closeTask(Long taskId) throws ResourcesNotFoundException, BadRequestException {
	    Task task = taskRepository.findById(taskId)
	            .orElseThrow(() -> new ResourcesNotFoundException("Task not found"));
	    User currentUser = getCurrentUser();

	    if (currentUser.getRole() != Role.ADMIN
	            && !task.getDepartments().stream().anyMatch(d -> currentUser.getDepartments().contains(d))) {
	        throw new BadRequestException("Unauthorized to close this task");
	    }
	    task.setStatus(TaskStatus.CLOSED);
	    task.setUpdatedAt(LocalDateTime.now());
	    task = taskRepository.save(task);

	    // === AUDIT LOG ===
	    logAudit(currentUser, "TASK_CLOSED", "Task", taskId, "Manually closed");

	    // === KAFKA ===
	    publishTaskEvent("TASK_CLOSED", task, "Task closed");

	    return taskMapper.toDto(task);
	}

	// ===========================================================
	// REQUEST EXTENSION
	// ===========================================================
	// ---------------------------------------------------------------
	// 1. requestExtension – use LocalDateTime for the new due date
	// ---------------------------------------------------------------
	@Override
	@PreAuthorize("hasAnyRole('HOD','TEACHER')")  // Fixed role
	public TaskDTO requestExtension(Long taskId, LocalDateTime newDueDate, String reason)
	        throws ResourcesNotFoundException, BadRequestException {

	    Task task = taskRepository.findById(taskId)
	            .orElseThrow(() -> new ResourcesNotFoundException("Task not found"));
	    User requester = getCurrentUser();

	    if (newDueDate.isBefore(task.getDueDate())) {
	        throw new BadRequestException("New due date must be after current due date");
	    }

	    task.setDueDate(newDueDate);
	    task.setUpdatedAt(LocalDateTime.now());
	    task = taskRepository.save(task);

	    // === AUDIT LOG ===
	    logAudit(requester, "EXTENSION_REQUESTED", "Task", taskId,
	            "New due: " + newDueDate + " | Reason: " + reason);

	    // === KAFKA ===
	    publishTaskEvent("EXTENSION_REQUESTED", task, "Extension requested: " + reason);

	    return taskMapper.toDto(task);
	}

	// ===========================================================
	// APPROVE/REJECT EXTENSION (Admin/HOD)
	// ===========================================================
	@Override
	@PreAuthorize("hasAnyRole('ADMIN')")
	public TaskDTO approveExtension(Long taskId) throws ResourcesNotFoundException, BadRequestException {
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new ResourcesNotFoundException("Task not found"));
		// Logic for approving extension (assume pending extension exists)
		publishTaskEvent("EXTENSION_APPROVED", task, "Extension approved");
		return taskMapper.toDto(task);
	}

	@Override
	@PreAuthorize("hasAnyRole('ADMIN')")
	public TaskDTO rejectExtension(Long taskId, String reason) throws ResourcesNotFoundException, BadRequestException {
		Task task = taskRepository.findById(taskId).orElseThrow(() -> new ResourcesNotFoundException("Task not found"));
		// Logic for rejecting
		publishTaskEvent("EXTENSION_REJECTED", task, "Extension rejected: " + reason);
		return taskMapper.toDto(task);
	}

	@Override
	@Transactional
	public TaskDTO startTask(Long taskId, Long userId) throws BadRequestException, UnauthorizedException {
	    if (taskId == null || userId == null) {
	        throw new IllegalArgumentException("Task ID and User ID are required");
	    }

	    Task task = taskRepository.findById(taskId)
	            .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

	    if (!task.getAssignedUserIds().contains(userId)) {
	        throw new UnauthorizedException("You are not assigned to this task");
	    }

	    if (task.getStatus() != TaskStatus.PENDING && task.getStatus() != TaskStatus.DELAYED) {
	        throw new BadRequestException("Task cannot be started. Current status: " + task.getStatus());
	    }

	    if (task.getStartedBy() != null) {
	        throw new BadRequestException("Task already started by " + task.getStartedBy().getFullName());
	    }

	    User starter = userRepository.getReferenceById(userId);
	    task.setStartedBy(starter);
	    task.setStartedAt(LocalDateTime.now());
	    task.setStatus(TaskStatus.IN_PROGRESS);
	    task.setUpdatedAt(LocalDateTime.now());

	    Task saved = taskRepository.save(task);

	    // === AUDIT LOG ===
	    logAudit(starter, "TASK_STARTED", "Task", taskId,
	            "Status: " + task.getStatus() + " → IN_PROGRESS");

	    // === KAFKA ===
	    publishTaskEvent("TASK_STARTED", saved, "Task started by " + starter.getFullName());

	    return taskMapper.toDto(saved);
	}
	// ===========================================================
	// PUBLISH TASK EVENT (Helper)
	// ===========================================================
	private void publishTaskEvent(String type, Task task, String message) {
		NotificationEvent event = new NotificationEvent();
		event.setType(type);
		event.setTaskId(task.getTaskId());
		event.setMessage(message + " - " + task.getTitle());
		event.setAssignedUserIds(task.getAssignedUsers().stream().map(User::getUserId).collect(Collectors.toList()));
		event.setDepartmentIds(
				task.getDepartments().stream().map(Department::getDepartmentId).collect(Collectors.toList()));
		event.setTimestamp(LocalDateTime.now());
		kafkaTemplate.send("task.events", task.getTaskId().toString(), event);
		log.info("Published event: {} for task {}", type, task.getTaskId());
	}

	// ... (rest of the methods remain the same)
	private void validatePayloadMinimal(TaskPayload payload) throws BadRequestException {
		if (payload.getTitle() == null || payload.getTitle().trim().isEmpty()) {
			throw new BadRequestException("Title is required");
		}
		if (payload.getDescription() == null || payload.getDescription().trim().isEmpty()) {
			throw new BadRequestException("Description is required");
		}
		if (payload.getDueDate() == null) {
			throw new BadRequestException("Due date is required");
		}
	}

	private void validateDatesForPayload(TaskPayload payload) throws BadRequestException {
	    LocalDateTime now = LocalDateTime.now();
	    LocalDate today = now.toLocalDate();

	    // === SET DEFAULT TIME TO 00:00:00 (MIDNIGHT) ===
	    LocalDateTime startDate = Optional.ofNullable(payload.getStartDate())
	            .map(date -> date.withHour(0).withMinute(0).withSecond(0).withNano(0))
	            .orElse(now.withHour(0).withMinute(0).withSecond(0).withNano(0)); // default to today 00:00

	    LocalDateTime dueDate = Optional.ofNullable(payload.getDueDate())
	            .map(date -> date.withHour(0).withMinute(0).withSecond(0).withNano(0))
	            .orElseThrow(() -> new BadRequestException("Due date is required"));

	    LocalDate startLocalDate = startDate.toLocalDate();
	    LocalDate dueLocalDate = dueDate.toLocalDate();

	    // === VALIDATION: No Sundays allowed ===
	    if (startLocalDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
	        throw new BadRequestException("Start date cannot be on Sunday");
	    }

	    if (dueLocalDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
	        throw new BadRequestException("Due date cannot be on Sunday");
	    }

	    // === PAST DATE CHECKS (Today is allowed) ===
	    if (startLocalDate.isBefore(today)) {
	        throw new BadRequestException("Start date cannot be in the past");
	    }

	    if (dueLocalDate.isBefore(today)) {
	        throw new BadRequestException("Due date cannot be in the past");
	    }

	    // === DUE DATE must be on or after START DATE ===
	    if (dueDate.isBefore(startDate)) {
	        throw new BadRequestException("Due date must be on or after the start date");
	    }

	    // === STATUS-SPECIFIC RULES ===
	    TaskStatus status = payload.getStatus();
	    if (status != null) {
	        switch (status) {
	            case PENDING:
	                // Due date can be today or in future → already validated above
	                // No extra restriction needed if today is allowed
	                break;

	            case UPCOMING:
	                // Start date must be strictly in the future (not today)
	                if (!startLocalDate.isAfter(today)) {
	                    throw new BadRequestException("For UPCOMING status, start date must be in the future (not today)");
	                }
	                break;
	            default:
	                throw new BadRequestException("Invalid task status: " + status);
	        }
	    }

	    // Optionally: Reassign normalized dates back (if you want to clean input)
	    payload.setStartDate(startDate);
	    payload.setDueDate(dueDate);
	}

	private void enforceHodDepartmentConstraint(User creator, Set<Department> departments) throws BadRequestException {
		boolean belongs = departments.stream().allMatch(d -> creator.getDepartments().contains(d));
		if (!belongs) {
			throw new BadRequestException("HOD can only create tasks for their own departments");
		}
	}

	private Set<Department> resolveDepartments(TaskPayload payload) throws BadRequestException {
		Set<Long> deptIds = new HashSet<>();
		if (payload.getDepartmentIds() != null) {
			deptIds.addAll(payload.getDepartmentIds());
		} else if (payload.getDepartmentId() != null) {
			deptIds.add(payload.getDepartmentId());
		}
		if (deptIds.isEmpty()) {
			throw new BadRequestException("At least one department must be selected");
		}
		List<Department> found = departmentRepository.findAllById(deptIds);
		if (found.size() != deptIds.size()) {
			throw new BadRequestException("Invalid department ID(s) provided");
		}
		return new HashSet<>(found);
	}

	private Set<User> resolveAssignedUsers(TaskPayload payload, Set<Department> departments, User creator)
			throws BadRequestException {
		Set<Long> assignedIds = new HashSet<>();
		if (payload.getAssignedToIds() != null)
			assignedIds.addAll(payload.getAssignedToIds());
		else if (payload.getAssignedToId() != null)
			assignedIds.add(payload.getAssignedToId());
		Set<User> assignedUsers = new HashSet<>();
		if (!assignedIds.isEmpty()) {
			List<User> users = userRepository.findAllById(assignedIds);
			Set<Long> foundIds = users.stream().map(User::getUserId).collect(Collectors.toSet());
			if (!foundIds.containsAll(assignedIds)) {
				throw new BadRequestException("One or more assigned users not found");
			}
			for (User u : users) {
				boolean belongs = u.getDepartments().stream().anyMatch(departments::contains);
				if (!belongs)
					throw new BadRequestException(
							"User " + u.getUsername() + " does not belong to selected departments");
				assignedUsers.add(u);
			}
		} else {
			// Auto-assign HODs of selected departments
			for (Department dept : departments) {
				Optional<User> hodOptional = userRepository.findByDepartmentsContainingAndRole(dept, Role.HOD);
				if (hodOptional.isEmpty()) {
					// Skip validation for "Administration" department
					if (!"administration".equalsIgnoreCase(dept.getName())) {
						throw new BadRequestException("No HOD found for department: " + dept.getName());
					}
					// Skip adding HOD for Administration if none exists
					continue;
				}
				// Add the HOD (single user)
				assignedUsers.add(hodOptional.get());
			}
		}
		if (assignedUsers.isEmpty()) {
			throw new BadRequestException("No valid users found for assignment");
		}
		return assignedUsers;
	}

	private void setApprovalBasedOnRole(Task task, User creator) {
		if (creator.getRole() == Role.HOD) {
			task.setApproved(false);
			task.setRequiresApproval(true);
		} else if (creator.getRole() == Role.ADMIN) {
			task.setApproved(true);
			task.setRequiresApproval(false);
		}
	}

	private void validateStatusChange(Task task, TaskStatus newStatus) throws BadRequestException {
	   
	    // 1. Only allow PENDING or UPCOMING
	    if (newStatus != TaskStatus.PENDING && newStatus != TaskStatus.UPCOMING) {
	        throw new BadRequestException(
	            "Status can only be changed to PENDING or UPCOMING. Received: " + newStatus);
	    }

	    // 2. Block change if task is IN_PROGRESS
	    if (task.getStatus() == TaskStatus.IN_PROGRESS) {
	        throw new BadRequestException(
	            "Task is IN_PROGRESS – status cannot be changed.");
	    }

	    // 3. Block change if task is CLOSED
	    if (task.getStatus() == TaskStatus.CLOSED) {
	        throw new BadRequestException(
	            "Task is CLOSED – status cannot be changed.");
	    }
	}
	
	private void updateTaskFields(Task task, TaskPayload payload)
			throws BadRequestException, ResourcesNotFoundException {
		if (payload.getTitle() != null)
			task.setTitle(payload.getTitle().trim());
		if (payload.getDescription() != null)
			task.setDescription(payload.getDescription().trim());
		if (payload.getDueDate() != null)
			task.setDueDate(payload.getDueDate());
		if (payload.getStartDate() != null)
			task.setStartDate(payload.getStartDate());
		if (payload.getStatus() != null) {
		    validateStatusChange(task, payload.getStatus()); // NEW
		    task.setStatus(payload.getStatus());
		}
		if (payload.getDepartmentIds() != null || payload.getDepartmentId() != null) {
			Set<Department> departments = resolveDepartments(payload);
			task.setDepartments(new HashSet<>(departments));
		}
		if (payload.getAssignedToIds() != null || payload.getAssignedToId() != null) {
			Set<User> assigned = resolveAssignedUsers(payload, new HashSet<>(task.getDepartments()), getCurrentUser());
			task.setAssignedUsers(new HashSet<>(assigned));
		}
	}

	private void validateApproverRole(User approver) {
		if (approver.getRole() != Role.ADMIN && approver.getRole() != Role.HOD) {
			throw new SecurityException("Only Admin or HOD can approve/reject tasks");
		}
	}
	
	private void logAudit(User user, String action, String entity, Long entityId, String details) {
	    AuditLog log = AuditLog.builder()
	            .user(user)
	            .action(action)
	            .entity(entity)
	            .entityId(entityId)
	            .details(details)
	            .build();

	    try {
	        auditLogRepository.save(log);
	    } catch (Exception e) {
//	    	log.info("Failed to save audit log: {}", e.getMessage()); 
	        // Don't fail the main transaction
	    }
	}


	


}