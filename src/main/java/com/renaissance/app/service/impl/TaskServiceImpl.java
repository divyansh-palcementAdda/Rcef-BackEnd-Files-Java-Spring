package com.renaissance.app.service.impl;

import com.renaissance.app.exception.BadRequestException;
import com.renaissance.app.exception.ResourcesNotFoundException;
import com.renaissance.app.mapper.TaskMapper;
import com.renaissance.app.model.*;
import com.renaissance.app.payload.TaskDTO;
import com.renaissance.app.payload.TaskPayload;
import com.renaissance.app.repository.DepartmentRepository;
import com.renaissance.app.repository.IUserRepository;
import com.renaissance.app.repository.TaskRepository;
import com.renaissance.app.service.interfaces.ITaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TaskServiceImpl implements ITaskService {

    private final TaskRepository taskRepository;
    private final IUserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final TaskMapper taskMapper;

    // ===============================
    // Get current logged-in user
    // ===============================
    private User getCurrentUser() throws ResourcesNotFoundException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new SecurityException("Unauthorized: User not authenticated");
        }
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResourcesNotFoundException("Authenticated user not found"));
    }

    // ===============================
    // CREATE TASK
    // ===============================
    @Override
    @PreAuthorize("hasAnyRole('ADMIN','HOD')")
    public TaskDTO createTask(TaskPayload payload) throws ResourcesNotFoundException, BadRequestException {
    	validatePayload(payload);
        log.debug(null);
        User creator = getCurrentUser();

        if (payload.getStatus() == TaskStatus.UPCOMING && payload.getStartDate() == null) {
            throw new BadRequestException("Start date is required for UPCOMING tasks");
        }

        Set<Department> departments = resolveDepartments(payload);
        if (creator.getRole() == Role.HOD) {
            if (departments.size() != 1 || !departments.contains(creator.getDepartment())) {
                throw new BadRequestException("HOD can only create tasks for their own department");
            }
        }

        Set<User> assignedUsers = resolveAssignedUsers(payload, departments, creator);
        if (creator.getRole() == Role.HOD) {
            Department hodDept = creator.getDepartment();
            for (User user : assignedUsers) {
                if (!hodDept.equals(user.getDepartment())) {
                    throw new BadRequestException("HOD can only assign tasks to users in their own department");
                }
            }
        }

        if (assignedUsers.contains(creator) && creator.getRole() == Role.ADMIN) {
            if (!"Adminstration".equalsIgnoreCase(creator.getDepartment().getName())) {
                throw new BadRequestException("Admin can only assign tasks to themselves if in Administration department");
            }
        }

        Task task = taskMapper.toEntityFromPayload(payload);
        task.setStartDate(payload.getStartDate());
        task.setCreatedBy(creator);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        task.setStatus(payload.getStatus() != null ? payload.getStatus() : TaskStatus.PENDING);

        setApprovalBasedOnRole(task, creator);

        task.setDepartments(departments);
        task.setAssignedUsers(assignedUsers);
        log.debug("Going to save Task");
        Task saved = taskRepository.save(task);
        log.info("Task '{}' created by user '{}'", saved.getTitle(), creator.getUsername());
        return taskMapper.toDto(saved);
    }

    // ===============================
    // UPDATE TASK
    // ===============================
    @Override
    @PreAuthorize("hasAnyRole('ADMIN','HOD')")
    public TaskDTO updateTask(Long taskId, TaskPayload payload) throws BadRequestException, ResourcesNotFoundException {
        if (taskId == null) throw new BadRequestException("Task ID is required");
        if (payload == null) throw new BadRequestException("Task payload is required");

        if (payload.getStatus() == TaskStatus.UPCOMING && payload.getStartDate() == null) {
            throw new BadRequestException("Start date is required for UPCOMING tasks");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourcesNotFoundException("Task not found with ID: " + taskId));

        updateTaskFields(task, payload);

        task.setUpdatedAt(LocalDateTime.now());
        Task updated = taskRepository.save(task);
        log.info("Task '{}' updated by user '{}'", updated.getTitle(), getCurrentUser().getUsername());
        return taskMapper.toDto(updated);
    }

    // ===============================
    // DELETE TASK
    // ===============================
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteTask(Long taskId) throws ResourcesNotFoundException, BadRequestException {
        if (taskId == null) throw new BadRequestException("Task ID is required");

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourcesNotFoundException("Task not found with ID: " + taskId));

        taskRepository.delete(task);
        log.info("Task '{}' deleted by user '{}'", task.getTitle(), getCurrentUser().getUsername());
    }

    // ===============================
    // GET TASK BY ID
    // ===============================
    @Override
    @Transactional(readOnly = true)
    public TaskDTO getTaskById(Long taskId) throws BadRequestException, ResourcesNotFoundException {
        if (taskId == null) throw new BadRequestException("Task ID is required");
        return taskRepository.findById(taskId)
                .map(taskMapper::toDto)
                .orElseThrow(() -> new ResourcesNotFoundException("Task not found with ID: " + taskId));
    }

    // ===============================
    // GET TASKS BY USER
    // ===============================
    @Override
    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByUser(Long userId) throws BadRequestException {
        if (userId == null) throw new BadRequestException("User ID is required");
        return taskRepository.findByAssignedUsers_UserId(userId).stream()
                .map(taskMapper::toDto)
                .collect(Collectors.toList());
    }

    // ===============================
    // GET TASKS BY DEPARTMENT
    // ===============================
    @Override
    @Transactional(readOnly = true)
    public List<TaskDTO> getTasksByDepartment(Long deptId) throws BadRequestException {
        if (deptId == null) throw new BadRequestException("Department ID is required");
        return taskRepository.findByDepartments_DepartmentId(deptId).stream()
                .map(taskMapper::toDto)
                .collect(Collectors.toList());
    }

    // ===============================
    // GET ALL TASKS
    // ===============================
    @Override
    @Transactional(readOnly = true)
    public List<TaskDTO> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(taskMapper::toDto)
                .collect(Collectors.toList());
    }

    // ===============================
    // GET TASKS BY STATUS
    // ===============================
    @Override
    @Transactional(readOnly = true)
    public List<TaskDTO> getAllTasksByStatus(TaskStatus status) {
        List<Task> tasks = (status == null)
                ? taskRepository.findAll()
                : taskRepository.findByStatus(status);
        return tasks.stream().map(taskMapper::toDto).collect(Collectors.toList());
    }

    // ===============================
    // APPROVE TASK
    // ===============================
    @Override
    @PreAuthorize("hasAnyRole('ADMIN','HOD')")
    public TaskDTO approveTask(Long taskId) throws ResourcesNotFoundException {
        Task task = getTaskByIdEntity(taskId);
        User approver = getCurrentUser();

        validateApproverRole(approver);

        task.setApproved(true);
        task.setRequiresApproval(false);
        task.setUpdatedAt(LocalDateTime.now());

        Task updated = taskRepository.save(task);
        log.info("Task '{}' approved by user '{}'", task.getTitle(), approver.getUsername());
        return taskMapper.toDto(updated);
    }

    // ===============================
    // REJECT TASK
    // ===============================
    @Override
    @PreAuthorize("hasAnyRole('ADMIN','HOD')")
    public TaskDTO rejectTask(Long taskId, String reason) throws BadRequestException, ResourcesNotFoundException {
        if (reason == null || reason.isBlank()) throw new BadRequestException("Rejection reason is required");

        Task task = getTaskByIdEntity(taskId);
        User approver = getCurrentUser();

        validateApproverRole(approver);

        task.setApproved(false);
        task.setRequiresApproval(false);
        task.setUpdatedAt(LocalDateTime.now());
        Task updated = taskRepository.save(task);
        log.info("Task '{}' rejected by user '{}' with reason '{}'", task.getTitle(), approver.getUsername(), reason);
        return taskMapper.toDto(updated);
    }

    // ===============================
    // ===============================
    // PRIVATE HELPER METHODS
    // ===============================

    private void validatePayload(TaskPayload payload) throws BadRequestException {
        if (payload == null) throw new BadRequestException("Task payload is required");
        if (payload.getTitle() == null || payload.getTitle().isBlank()) throw new BadRequestException("Task title is required");
    }

    private Set<Department> resolveDepartments(TaskPayload payload) throws BadRequestException {
        Set<Long> deptIds = payload.getDepartmentIds() != null && !payload.getDepartmentIds().isEmpty()
                ? payload.getDepartmentIds()
                : (payload.getDepartmentId() != null ? Collections.singleton(payload.getDepartmentId()) : Collections.emptySet());
        if (deptIds.isEmpty()) throw new BadRequestException("At least one department must be selected");

        List<Department> departments = departmentRepository.findAllById(deptIds);
        if (departments.size() != deptIds.size()) throw new BadRequestException("One or more department IDs are invalid");

        // ✅ Create a NEW HashSet instead of relying on Hibernate PersistentSet
        Set<Department> resolved = new HashSet<>();
        for (Department d : departments) {
            resolved.add(d);
        }
        return resolved;
    }


    private Set<User> resolveAssignedUsers(TaskPayload payload, Set<Department> departments, User creator) throws BadRequestException {
        Set<Long> assignedToIds = payload.getAssignedToIds() != null && !payload.getAssignedToIds().isEmpty()
                ? payload.getAssignedToIds()
                : (payload.getAssignedToId() != null ? Collections.singleton(payload.getAssignedToId()) : Collections.emptySet());

        Set<User> assignedUsers = new HashSet<>();
        System.err.println(payload.getAssignedToIds());
        System.err.println(payload.getAssignedToId());

        if (!assignedToIds.isEmpty()) {
            List<User> users = userRepository.findAllById(assignedToIds);
            for (User user : users) {
                if (user.getDepartment() == null || !departments.contains(user.getDepartment())) {
                    throw new BadRequestException("User " + user.getUsername() + " does not belong to selected departments");
                }
                assignedUsers.add(user); // ✅ Add after validation
            }
        } else {
            for (Department dept : departments) {
                List<User> hods = userRepository.findByDepartmentAndRole(dept, Role.HOD);
                if (hods.isEmpty() && !dept.getName().toUpperCase().contains("ADMIN")) throw new BadRequestException("No HOD found for department: " + dept.getName());
                assignedUsers.addAll(hods);
            }
        }

        if (assignedUsers.isEmpty()) throw new BadRequestException("No valid users found for task assignment");
        return assignedUsers;
    }


    private void setApprovalBasedOnRole(Task task, User creator) {
        if (creator.getRole() == Role.HOD) {
            task.setApproved(false);
            task.setRequiresApproval(true);
        } else {
            task.setApproved(true);
            task.setRequiresApproval(false);
        }
    }

    private void updateTaskFields(Task task, TaskPayload payload) throws BadRequestException, ResourcesNotFoundException {
        if (payload.getTitle() != null && !payload.getTitle().isBlank()) task.setTitle(payload.getTitle().trim());
        if (payload.getDescription() != null) task.setDescription(payload.getDescription().trim());
        if (payload.getDueDate() != null) task.setDueDate(payload.getDueDate());
        if (payload.getStartDate() != null) task.setStartDate(payload.getStartDate());
        if (payload.getStatus() != null) task.setStatus(payload.getStatus());

        Set<User> assignedUsers = resolveAssignedUsers(payload, task.getDepartments(), getCurrentUser());
        task.setAssignedUsers(assignedUsers);

        Set<Department> departments = resolveDepartments(payload);
        task.setDepartments(departments);
    }

    private Task getTaskByIdEntity(Long taskId) throws ResourcesNotFoundException {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourcesNotFoundException("Task not found with ID: " + taskId));
    }

    private void validateApproverRole(User approver) {
        if (!(approver.getRole() == Role.ADMIN || approver.getRole() == Role.HOD)) {
            throw new SecurityException("Only Admin or HOD can approve/reject tasks");
        }
    }
}