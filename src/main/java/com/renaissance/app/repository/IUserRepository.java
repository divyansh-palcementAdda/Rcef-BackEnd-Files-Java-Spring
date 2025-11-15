package com.renaissance.app.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.renaissance.app.model.Department;
import com.renaissance.app.model.Role;
import com.renaissance.app.model.User;
import com.renaissance.app.model.UserStatus;

@Repository
public interface IUserRepository extends JpaRepository<User, Long> {

    // ---------------------------------------------------------
    // Basic Finders
    // ---------------------------------------------------------
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    // ---------------------------------------------------------
    // Role & Department Queries (Many-to-Many safe)
    // ---------------------------------------------------------

    // 🔹 Find all users having a given role in a specific department
    Optional<User> findByDepartmentsContainingAndRole(Department department, Role role);
    
    boolean existsByRoleAndDepartmentsContaining(Role role, Department department);


    // 🔹 Find all users of a specific role (irrespective of department)
    List<User> findByRole(Role role);

    // 🔹 Find all users belonging to a given department ID
    List<User> findByDepartments_DepartmentId(Long departmentId);

    // ---------------------------------------------------------
    // Status-based Counters
    // ---------------------------------------------------------
    long countByStatus(UserStatus status);

    List<User> findByStatus(UserStatus status);

    // 🔹 Count users belonging to a specific department (by ID)
    long countByDepartments_DepartmentId(Long departmentId);

    // 🔹 Count active/inactive users by department
    long countByDepartments_DepartmentIdAndStatus(Long departmentId, UserStatus status);

    // 🔹 Count users by department entity (for service layer convenience)
    long countByDepartmentsContainingAndStatus(Department department, UserStatus status);

    @Query("select u.userId from User u join u.departments d where d.departmentId = :deptId and u.role = 'HOD'")
    List<Long> findHodIdsByDepartment(@Param("deptId") Long deptId);

	boolean existsByDepartmentsContainingAndRole(Department dept, Role hod);

	Optional<User> findByDepartmentsContaining(Department dept);
}
