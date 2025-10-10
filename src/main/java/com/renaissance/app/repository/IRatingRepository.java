package com.renaissance.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.renaissance.app.model.Department;
import com.renaissance.app.model.Rating;
import com.renaissance.app.model.Task;
import com.renaissance.app.model.User;

public interface IRatingRepository extends JpaRepository<Rating, Long> {
	List<Rating> findByRatedUser_UserId(Long userId);

	List<Rating> findByRatedDepartment_DepartmentId(Long departmentId);

	List<Rating> findByTask_TaskId(Long taskId);

	boolean existsByRatedUserAndGivenBy(User ratedUser, User givenBy);

	boolean existsByRatedDepartmentAndGivenBy(Department ratedDepartment, User givenBy); // Corrected to use Department

	boolean existsByTaskAndGivenBy(Task task, User givenBy);
}