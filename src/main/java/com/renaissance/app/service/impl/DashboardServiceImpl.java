package com.renaissance.app.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.renaissance.app.model.Department;
import com.renaissance.app.model.TaskStatus;
import com.renaissance.app.model.User;
import com.renaissance.app.model.UserStatus;
import com.renaissance.app.payload.DashboardDto;
import com.renaissance.app.repository.DepartmentRepository;
import com.renaissance.app.repository.IUserRepository;
import com.renaissance.app.repository.TaskRepository;
import com.renaissance.app.service.interfaces.IDashboardService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements IDashboardService {

	@Autowired
    private  IUserRepository userRepository;
	@Autowired
	private TaskRepository taskRepository;
	@Autowired
	private DepartmentRepository departmentRepository;

    @Override
    public DashboardDto getDashboardData(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        String role = user.getRole().name(); // ADMIN / HOD / TEACHER
        Long totalTask = 0L;
        Long pendingTask = 0L;
        Long delayedTask = 0L;
        Long completedTask = 0L;
        Long upcomingTask = 0L;
        Long rfc = 0L;
        Long rfe = 0L;

        Long activeUsers = null;
        Long totalUsers = null;
        Long totalDepartments = null;
        Long selfTask =0L;
        
        if (role.equals("ADMIN")) {
            // 🔹 Task counts
            totalTask = taskRepository.count();
            pendingTask = taskRepository.countByStatus(TaskStatus.PENDING);
            delayedTask = taskRepository.countByStatus(TaskStatus.DELAYED);
            completedTask = taskRepository.countByStatus(TaskStatus.CLOSED);
            upcomingTask = taskRepository.countByStatus(TaskStatus.UPCOMING);
            rfc = taskRepository.countByStatus(TaskStatus.REQUEST_FOR_CLOSURE);
            rfe = taskRepository.countByStatus(TaskStatus.REQUEST_FOR_EXTENSION);
            
            // 🔹 User counts
            activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
            totalUsers = userRepository.count();
            //self task counts
            selfTask = taskRepository.countByAssignedTo(user);
            // 🔹 Department counts
            totalDepartments = departmentRepository.count();
//            activeDepartments = departmentRepository.countByActiveTrue();

        } else if (role.equals("HOD")) {
            Department dept = user.getDepartment();

            totalTask = taskRepository.countByDepartment(dept);
            pendingTask = taskRepository.countByDepartmentAndStatus(dept, TaskStatus.PENDING);
            delayedTask = taskRepository.countByDepartmentAndStatus(dept, TaskStatus.DELAYED);
            completedTask = taskRepository.countByDepartmentAndStatus(dept, TaskStatus.CLOSED);
            upcomingTask = taskRepository.countByDepartmentAndStatus(dept, TaskStatus.UPCOMING);
            rfc = taskRepository.countByDepartmentAndStatus(dept, TaskStatus.REQUEST_FOR_CLOSURE);
            rfe = taskRepository.countByDepartmentAndStatus(dept, TaskStatus.REQUEST_FOR_EXTENSION);

            // 🔹 HOD can only see users in their department
            activeUsers = userRepository.countByDepartmentAndStatus(dept, UserStatus.ACTIVE);
            totalUsers = userRepository.countByDepartment(dept);

        } else if (role.equals("TEACHER")) {
            User teacher = user;

            totalTask = taskRepository.countByAssignedTo(teacher);
            pendingTask = taskRepository.countByAssignedToAndStatus(teacher, TaskStatus.PENDING);
            delayedTask = taskRepository.countByAssignedToAndStatus(teacher, TaskStatus.DELAYED);
            completedTask = taskRepository.countByAssignedToAndStatus(teacher, TaskStatus.CLOSED);
            upcomingTask = taskRepository.countByAssignedToAndStatus(teacher, TaskStatus.UPCOMING);
            rfc = taskRepository.countByAssignedToAndStatus(teacher, TaskStatus.REQUEST_FOR_CLOSURE);
            rfe = taskRepository.countByAssignedToAndStatus(teacher, TaskStatus.REQUEST_FOR_EXTENSION);

            // 🔹 Teachers don't see user or department counts
        }

        return DashboardDto.builder()
                .totalTask(totalTask)
                .pendingTask(pendingTask)
                .delayedTask(delayedTask)
                .completedTask(completedTask)
                .upcomingTask(upcomingTask)
                .requestForClosure(rfc)
                .requestForExtension(rfe)
                .activeUsers(activeUsers)
                .totalUsers(totalUsers)
                .totalDepartments(totalDepartments)
                .userName(username).email(user.getEmail())
//                .activeDepartments(activeDepartments)
                .selfTask(selfTask)
                .loggedInRole(role)
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .build();
    }
}
