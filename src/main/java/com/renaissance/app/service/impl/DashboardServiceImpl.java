package com.renaissance.app.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renaissance.app.exception.ResourcesNotFoundException;
import com.renaissance.app.model.Department;
import com.renaissance.app.model.Role;
import com.renaissance.app.model.TaskStatus;
import com.renaissance.app.model.User;
import com.renaissance.app.model.UserStatus;
import com.renaissance.app.payload.DashboardDto;
import com.renaissance.app.repository.DepartmentRepository;
import com.renaissance.app.repository.IUserRepository;
import com.renaissance.app.repository.TaskRepository;
import com.renaissance.app.service.interfaces.IDashboardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardServiceImpl implements IDashboardService {

    private final IUserRepository userRepository;
    private final TaskRepository taskRepository;
    private final DepartmentRepository departmentRepository;

    @Override
    public DashboardDto getDashboardData(String username) throws ResourcesNotFoundException {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be null or empty");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourcesNotFoundException("User not found for username: " + username));

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new SecurityException("Inactive user cannot access dashboard");
        }

        Role role = user.getRole();
        if (role == null) {
            throw new IllegalStateException("User role is missing for: " + username);
        }

        List<Department> departments = user.getDepartments();

        DashboardDto.DashboardDtoBuilder builder = DashboardDto.builder()
                .userName(user.getUsername())
                .email(user.getEmail())
                .loggedInRole(role.name())
                .departmentName(
                        departments != null && !departments.isEmpty()
                                ? String.join(", ", departments.stream().map(Department::getName).toList())
                                : null
                );

        switch (role) {
            case ADMIN -> populateAdminStats(builder, user);
            case HOD -> populateHodStats(builder, user);
            case TEACHER -> populateTeacherStats(builder, user);
            default -> log.warn("Unhandled role type: {}", role);
        }

        DashboardDto dto = builder.build();
        log.info("✅ Dashboard data loaded for user: {} ({})", username, role);
        return dto;
    }

    // ============================ ADMIN DASHBOARD =============================
    private void populateAdminStats(DashboardDto.DashboardDtoBuilder b, User u) {
        try {
            b.totalTask(taskRepository.count())
                    .pendingTask(taskRepository.countByStatus(TaskStatus.PENDING))
                    .delayedTask(taskRepository.countByStatus(TaskStatus.DELAYED))
                    .activeTask(taskRepository.countByStatus(TaskStatus.IN_PROGRESS))
                    .completedTask(taskRepository.countByStatus(TaskStatus.CLOSED))
                    .upcomingTask(taskRepository.countByStatus(TaskStatus.UPCOMING))
                    .requestForClosure(taskRepository.countByStatus(TaskStatus.REQUEST_FOR_CLOSURE))
                    .requestForExtension(taskRepository.countByStatus(TaskStatus.REQUEST_FOR_EXTENSION))
                    .extendedTask(taskRepository.countByStatus(TaskStatus.EXTENDED))
                    .activeUsers(userRepository.countByStatus(UserStatus.ACTIVE))
                    .totalUsers(userRepository.count())
                    .totalDepartments(departmentRepository.count())
                    .selfTask(taskRepository.countByAssignedUsers_UserId(u.getUserId()));
        } catch (Exception e) {
            log.error("❌ Error populating admin dashboard for user {}", u.getUsername(), e);
            throw new RuntimeException("Failed to load admin dashboard", e);
        }
    }

    // ============================ HOD DASHBOARD =============================
    private void populateHodStats(DashboardDto.DashboardDtoBuilder b, User hod) {
        List<Department> departments = hod.getDepartments();
        if (departments == null || departments.isEmpty()) {
            throw new IllegalStateException("HOD must be associated with at least one department");
        }

        try {
            long totalTasks = 0,activeTasks=0, pending = 0, delayed = 0, completed = 0, upcoming = 0, reqClosure = 0, reqExt = 0, extended = 0;
            long activeUsers = 0, totalUsers = 0;

            for (Department dept : departments) {
                Long deptId = dept.getDepartmentId();

                totalTasks += taskRepository.countByDepartments_DepartmentId(deptId);
                pending += taskRepository.countByDepartments_DepartmentIdAndStatus(deptId, TaskStatus.PENDING);
                delayed += taskRepository.countByDepartments_DepartmentIdAndStatus(deptId, TaskStatus.DELAYED);
                completed += taskRepository.countByDepartments_DepartmentIdAndStatus(deptId, TaskStatus.CLOSED);
                upcoming += taskRepository.countByDepartments_DepartmentIdAndStatus(deptId, TaskStatus.UPCOMING);
                reqClosure += taskRepository.countByDepartments_DepartmentIdAndStatus(deptId, TaskStatus.REQUEST_FOR_CLOSURE);
                reqExt += taskRepository.countByDepartments_DepartmentIdAndStatus(deptId, TaskStatus.REQUEST_FOR_EXTENSION);
                extended += taskRepository.countByDepartments_DepartmentIdAndStatus(deptId, TaskStatus.EXTENDED);
                activeTasks+=taskRepository.countByDepartments_DepartmentIdAndStatus(deptId, TaskStatus.IN_PROGRESS);
                
                activeUsers += userRepository.countByDepartments_DepartmentIdAndStatus(dept.getDepartmentId(), UserStatus.ACTIVE);
                totalUsers += userRepository.countByDepartments_DepartmentId(dept.getDepartmentId());

            }

            b.totalTask(totalTasks)
                    .pendingTask(pending)
                    .delayedTask(delayed)
                    .completedTask(completed)
                    .upcomingTask(upcoming)
                    .requestForClosure(reqClosure)
                    .requestForExtension(reqExt)
                    .extendedTask(extended)
                    .activeUsers(activeUsers)
                    .activeTask(activeTasks)
                    .totalUsers(totalUsers);

        } catch (Exception e) {
            log.error("❌ Error populating HOD dashboard for user {}", hod.getUsername(), e);
            throw new RuntimeException("Failed to load HOD dashboard", e);
        }
    }

    // ============================ TEACHER DASHBOARD =============================
    private void populateTeacherStats(DashboardDto.DashboardDtoBuilder b, User teacher) {
        try {
            Long teacherId = teacher.getUserId();

            b.totalTask(taskRepository.countByAssignedUsers_UserId(teacherId))
                    .pendingTask(taskRepository.countByAssignedUsers_UserIdAndStatus(teacherId, TaskStatus.PENDING))
                    .delayedTask(taskRepository.countByAssignedUsers_UserIdAndStatus(teacherId, TaskStatus.DELAYED))
                    .completedTask(taskRepository.countByAssignedUsers_UserIdAndStatus(teacherId, TaskStatus.CLOSED))
                    .upcomingTask(taskRepository.countByAssignedUsers_UserIdAndStatus(teacherId, TaskStatus.UPCOMING))
                    .activeTask(taskRepository.countByAssignedUsers_UserIdAndStatus(teacherId, TaskStatus.IN_PROGRESS))
                    .requestForClosure(taskRepository.countByAssignedUsers_UserIdAndStatus(teacherId, TaskStatus.REQUEST_FOR_CLOSURE))
                    .requestForExtension(taskRepository.countByAssignedUsers_UserIdAndStatus(teacherId, TaskStatus.REQUEST_FOR_EXTENSION))
         
                    .extendedTask(taskRepository.countByAssignedUsers_UserIdAndStatus(teacherId, TaskStatus.EXTENDED));
        } catch (Exception e) {
            log.error("❌ Error populating Teacher dashboard for user {}", teacher.getUsername(), e);
            throw new RuntimeException("Failed to load Teacher dashboard", e);
        }
    }
}
