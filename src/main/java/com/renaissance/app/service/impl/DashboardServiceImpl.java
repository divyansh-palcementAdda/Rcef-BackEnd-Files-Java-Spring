package com.renaissance.app.service.impl;

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

        DashboardDto.DashboardDtoBuilder builder = DashboardDto.builder()
                .userName(user.getUsername())
                .email(user.getEmail())
                .loggedInRole(role.name())
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null);

        // Role-based data aggregation
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
        Department dept = hod.getDepartment();
        if (dept == null) {
            throw new IllegalStateException("HOD must be associated with a department");
        }

        try {
            Long deptId = dept.getDepartmentId();

            b.totalTask(taskRepository.countByDepartments_DepartmentId(deptId))
                    .pendingTask(taskRepository.countByDepartments_DepartmentIdAndStatus(deptId, TaskStatus.PENDING))
                    .delayedTask(taskRepository.countByDepartments_DepartmentIdAndStatus(deptId, TaskStatus.DELAYED))
                    .completedTask(taskRepository.countByDepartments_DepartmentIdAndStatus(deptId, TaskStatus.CLOSED))
                    .upcomingTask(taskRepository.countByDepartments_DepartmentIdAndStatus(deptId, TaskStatus.UPCOMING))
                    .requestForClosure(taskRepository.countByDepartments_DepartmentIdAndStatus(deptId, TaskStatus.REQUEST_FOR_CLOSURE))
                    .requestForExtension(taskRepository.countByDepartments_DepartmentIdAndStatus(deptId, TaskStatus.REQUEST_FOR_EXTENSION))
                    .extendedTask(taskRepository.countByDepartments_DepartmentIdAndStatus(deptId, TaskStatus.EXTENDED))
                    .activeUsers(userRepository.countByDepartmentAndStatus(dept, UserStatus.ACTIVE))
                    .totalUsers(userRepository.countByDepartment(dept));
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
                    .requestForClosure(taskRepository.countByAssignedUsers_UserIdAndStatus(teacherId, TaskStatus.REQUEST_FOR_CLOSURE))
                    .requestForExtension(taskRepository.countByAssignedUsers_UserIdAndStatus(teacherId, TaskStatus.REQUEST_FOR_EXTENSION))
                    .extendedTask(taskRepository.countByAssignedUsers_UserIdAndStatus(teacherId, TaskStatus.EXTENDED));
        } catch (Exception e) {
            log.error("❌ Error populating Teacher dashboard for user {}", teacher.getUsername(), e);
            throw new RuntimeException("Failed to load Teacher dashboard", e);
        }
    }
}
