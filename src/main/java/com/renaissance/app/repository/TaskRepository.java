package com.renaissance.app.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.renaissance.app.model.Department;
import com.renaissance.app.model.Task;
import com.renaissance.app.model.TaskStatus;
import com.renaissance.app.model.User;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // 🔹 Find tasks assigned to a specific user (Many-to-Many)
    List<Task> findByAssignedUsers_UserId(Long userId);

    // 🔹 Find tasks by department (Many-to-Many)
    List<Task> findByDepartments_DepartmentId(Long departmentId);

    // 🔹 Filter by status
    List<Task> findByStatus(TaskStatus status);

    // 🔹 Filter by user and status
    List<Task> findByAssignedUsers_UserIdAndStatus(Long userId, TaskStatus status);

    // 🔹 Filter by department and status
    List<Task> findByDepartments_DepartmentIdAndStatus(Long departmentId, TaskStatus status);

    // 🔹 Tasks created by a specific user
    List<Task> findByCreatedBy_UserId(Long userId);

    // 🔹 Tasks requiring approval
    List<Task> findByRequiresApprovalTrue();

    // ==============================
    // 🔹 COUNT QUERIES (for dashboards/statistics)
    // ==============================

    // ✅ Admin-level: global count by status
    Long countByStatus(TaskStatus status);

    // ✅ HOD-level: department-based counts
    Long countByDepartments_DepartmentId(Long departmentId);

    Long countByDepartments_DepartmentIdAndStatus(Long departmentId, TaskStatus status);

    // ✅ Teacher-level: user-based counts
    Long countByAssignedUsers_UserId(Long userId);

    Long countByAssignedUsers_UserIdAndStatus(Long userId, TaskStatus status);
    
    Page<Task> findByAssignedUsers_UserId(Long userId, Pageable pageable);

	List<Task> findByStartDate(LocalDate localDate);

	List<Task> findByDueDate(LocalDate plusDays);

	List<Task> findByDueDateBefore(LocalDate now);

	List<Task> findByDueDateBeforeAndStatusNot(LocalDate date, TaskStatus status);

	List<Task> findByRequiresApprovalTrueAndApprovedFalse();

	Optional<User> findByDepartmentsContaining(Department dept);

	Optional<User> findByAssignedUsersContaining(User user);

	boolean existsByAssignedUsersContaining(User targetUser);

}