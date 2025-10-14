package com.renaissance.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.renaissance.app.model.Department;
import com.renaissance.app.model.Task;
import com.renaissance.app.model.TaskStatus;
import com.renaissance.app.model.User;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
	List<Task> findByAssignedTo_UserId(Long userId);
//	 int countByAssignedToIdAndStatus(Long assignedToId, TaskStatus status);
	List<Task> findByDepartment_DepartmentId(Long departmentId);

	List<Task> findByStatus(TaskStatus status);

	List<Task> findByAssignedTo_UserIdAndStatus(Long userId, TaskStatus status);

	List<Task> findByDepartment_DepartmentIdAndStatus(Long departmentId, TaskStatus status);

	List<Task> findByCreatedBy_UserId(Long userId); // ✅ add this for HOD reports
	
//	Long countByStatus(String status);

//	Long countByDepartment(Long department_id);

//	Long countByDepartmentAndStatus(Long departmentId, String status);

//	Long countByAssignedTo(Long userId);

//	Long countByAssignedUserAndStatus(Long userId, String status);
	
	
	    // 🔹 For Admin (all tasks)
	    Long countByStatus(TaskStatus status);

	    // 🔹 For HOD (department-based)
	    Long countByDepartment(Department department);
	    Long countByDepartmentAndStatus(Department department, TaskStatus status);

	    // 🔹 For Teacher (user-based)
	    Long countByAssignedTo(User user);
	    Long countByAssignedToAndStatus(User user, TaskStatus status);
	


}
