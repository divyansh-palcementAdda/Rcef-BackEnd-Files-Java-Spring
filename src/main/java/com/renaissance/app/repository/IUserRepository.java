package com.renaissance.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.renaissance.app.model.Department;
import com.renaissance.app.model.Role;
import com.renaissance.app.model.User;
import com.renaissance.app.model.UserStatus;

@Repository
public interface IUserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsername(String username);

	Optional<User> findByEmail(String email);

	List<User> findByRole(Role role);

	List<User> findByDepartment_DepartmentId(Long departmentId);

	boolean existsByEmail(String email);

	boolean existsByUsername(String username);

	Long countByStatus(UserStatus status);
	
	List<User> findByStatus(UserStatus status);

	Long countByDepartmentAndStatus(Department department, UserStatus status);

	Long countByDepartment(Department department);
}
