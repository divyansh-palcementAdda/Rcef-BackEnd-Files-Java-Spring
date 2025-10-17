package com.renaissance.app.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.renaissance.app.exception.AccessDeniedException;
import com.renaissance.app.mapper.UserMapper;
import com.renaissance.app.model.Department;
import com.renaissance.app.model.Role;
import com.renaissance.app.model.TaskStatus;
import com.renaissance.app.model.User;
import com.renaissance.app.model.UserStatus;
import com.renaissance.app.payload.UserDTO;
import com.renaissance.app.payload.UserRequest;
import com.renaissance.app.repository.DepartmentRepository;
import com.renaissance.app.repository.IUserRepository;
import com.renaissance.app.repository.TaskRepository;
import com.renaissance.app.security.UserSecurityUtil;
import com.renaissance.app.service.interfaces.IUserService;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class UserServiceImpl implements IUserService {
	private final IUserRepository userRepository;
	private final DepartmentRepository departmentRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserSecurityUtil securityUtil;
	private final UserMapper userMapper;
	private final TaskRepository taskRepository;

	public UserServiceImpl(IUserRepository userRepository, DepartmentRepository departmentRepository,
			PasswordEncoder passwordEncoder, UserSecurityUtil securityUtil, UserMapper userMapper,
			TaskRepository taskRepository) {
		this.userRepository = userRepository;
		this.departmentRepository = departmentRepository;
		this.passwordEncoder = passwordEncoder;
		this.securityUtil = securityUtil;
		this.userMapper = userMapper;
		this.taskRepository = taskRepository;
	}

	@Override
	@PreAuthorize("hasAnyRole('ADMIN','HOD')")
	public UserDTO updateUser(Long userId, UserRequest request) throws AccessDeniedException {
		if (userId == null) {
			throw new IllegalArgumentException("userId is required");
		}
		if (request == null) {
			throw new IllegalArgumentException("User request is required");
		}
		User existing = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
		if (!canAccessUser(userId)) {
			throw new AccessDeniedException("You are not authorized to update this user");
		}
		if (request.getFullName() != null && !request.getFullName().isBlank()) {
			existing.setFullName(request.getFullName().trim());
		}
		if (request.getUsername() != null && !request.getUsername().isBlank()) {
			existing.setUsername(request.getUsername().trim());
		}
		if (request.getEmail() != null && !request.getEmail().isBlank()) {
			existing.setEmail(request.getEmail().trim());
		}
		if (request.getPassword() != null && !request.getPassword().isBlank()) {
			existing.setPassword(passwordEncoder.encode(request.getPassword()));
		}
		if (request.getRole() != null) {
			if (!securityUtil.hasAdminRole()) {
				throw new AccessDeniedException("Only admin can change user roles");
			}
			existing.setRole(request.getRole());
		}
		if (request.getDepartmentId() != null) {
			Department dept = departmentRepository.findById(request.getDepartmentId())
					.orElseThrow(() -> new RuntimeException("Department not found"));
			existing.setDepartment(dept);
		}
		existing.setStatus(UserStatus.ACTIVE);
		existing.setUpdatedAt(LocalDateTime.now());
		User saved = userRepository.save(existing);
		return userMapper.toDto(saved);
	}

	@Override
	public UserDTO activeUserById(Long userId) {
		if (userId == null) {
			throw new IllegalArgumentException("userId is required");
		}
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
		Long currentUserId = securityUtil.getCurrentUserId();
		if (currentUserId != null && currentUserId.equals(user.getUserId())) {
			throw new SecurityException("Cannot activate yourself");
		}
		if (user.getStatus().equals(UserStatus.ACTIVE)) {
			throw new IllegalArgumentException("User is already ACTIVE");
		}
		user.setStatus(UserStatus.ACTIVE);
		user.setUpdatedAt(LocalDateTime.now());
		User saved = userRepository.save(user);
		log.info("User {} marked ACTIVE by {}", userId, currentUserId);
		return userMapper.toDto(saved);
	}

	@Override
	@PreAuthorize("hasRole('ADMIN')")
	public void deleteUser(Long userId) {
		if (userId == null) {
			throw new IllegalArgumentException("userId is required");
		}
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
		Long currentUserId = securityUtil.getCurrentUserId();
		if (currentUserId != null && currentUserId.equals(user.getUserId())) {
			throw new SecurityException("Cannot delete yourself");
		}
		if (user.getRole() == Role.ADMIN) {
			throw new SecurityException("Cannot delete another admin");
		}
		if (user.getStatus().equals(UserStatus.INACTIVE)) {
			throw new IllegalArgumentException("User is already inactive");
		}
		user.setStatus(UserStatus.INACTIVE);
		user.setUpdatedAt(LocalDateTime.now());
		userRepository.save(user);
		log.info("User {} marked INACTIVE by {}", userId, currentUserId);
	}

	@Override
	public UserDTO getUserById(Long userId) throws AccessDeniedException {
		if (userId == null) {
			throw new IllegalArgumentException("userId is required");
		}
		if (!canAccessUser(userId)) {
			throw new AccessDeniedException("Unauthorized");
		}
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
		UserDTO userDTO = userMapper.toDto(user);
		userDTO.setPendingTasks(
				taskRepository.countByAssignedUsers_UserIdAndStatus(user.getUserId(), TaskStatus.PENDING));
		userDTO.setUpcomingTasks(
				taskRepository.countByAssignedUsers_UserIdAndStatus(user.getUserId(), TaskStatus.UPCOMING));
		userDTO.setDelayedTasks(
				taskRepository.countByAssignedUsers_UserIdAndStatus(user.getUserId(), TaskStatus.DELAYED));
		userDTO.setClosedTasks(
				taskRepository.countByAssignedUsers_UserIdAndStatus(user.getUserId(), TaskStatus.CLOSED));
		return userDTO;
	}

	@Override
	public List<UserDTO> getAllUsers() throws AccessDeniedException {
		if (!securityUtil.hasAdminRole()) {
			throw new AccessDeniedException("Only admins allowed");
		}
		return userRepository.findAll().stream().map(userMapper::toDto).collect(Collectors.toList());
	}

	@Override
	public List<UserDTO> getAllUserByRole(Role role) throws AccessDeniedException {
		if (role == null) {
			throw new IllegalArgumentException("role is required");
		}
		if (!(securityUtil.hasAdminRole() || securityUtil.hasHodRole())) {
			throw new AccessDeniedException("Not allowed");
		}
		List<User> users = userRepository.findByRole(role);
		if (securityUtil.hasHodRole() && !securityUtil.hasAdminRole()) {
			Long currentId = securityUtil.getCurrentUserId();
			Long hodDeptId = userRepository.findById(currentId)
					.map(u -> u.getDepartment() != null ? u.getDepartment().getDepartmentId() : null).orElse(null);
			if (hodDeptId != null) {
				users = users.stream()
						.filter(u -> u.getDepartment() != null && hodDeptId.equals(u.getDepartment().getDepartmentId()))
						.collect(Collectors.toList());
			} else {
				users = List.of();
			}
		}
		return users.stream().map(userMapper::toDto).collect(Collectors.toList());
	}

	@Override
	public void validateUser(String email) {
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("email is required");
		}
		Optional<User> userOpt = userRepository.findByEmail(email);
		if (userOpt.isPresent()) {
			User user = userOpt.get();
			user.setEmailVerified(true);
			user.setVerificationToken(null);
			user.setUpdatedAt(LocalDateTime.now());
			userRepository.save(user);
		} else {
			throw new RuntimeException("User with email not found");
		}
	}

	@Override
	public List<UserDTO> getUsersByDepartment(Long departmentId) throws AccessDeniedException {
		if (departmentId == null) {
			throw new IllegalArgumentException("departmentId is required");
		}
		if (securityUtil.hasHodRole() && !securityUtil.hasAdminRole()) {
			Long currentId = securityUtil.getCurrentUserId();
			Long hodDeptId = userRepository.findById(currentId)
					.map(u -> u.getDepartment() != null ? u.getDepartment().getDepartmentId() : null).orElse(null);
			if (hodDeptId == null || !hodDeptId.equals(departmentId)) {
				throw new AccessDeniedException("HOD can only view their own department users");
			}
		} else if (!securityUtil.hasAdminRole() && !securityUtil.hasHodRole()) {
			throw new AccessDeniedException("Not allowed");
		}
		List<User> users = userRepository.findByDepartment_DepartmentId(departmentId);
		return users.stream().map(userMapper::toDto).collect(Collectors.toList());
	}

	private boolean canAccessUser(Long targetUserId) {
		Long currentId = securityUtil.getCurrentUserId();
		if (currentId == null) {
			return false;
		}
		if (currentId.equals(targetUserId) || securityUtil.hasAdminRole()) {
			return true;
		}
		if (securityUtil.hasHodRole()) {
			Long hodDeptId = userRepository.findById(currentId)
					.map(u -> u.getDepartment() != null ? u.getDepartment().getDepartmentId() : null).orElse(null);
			return hodDeptId != null && userRepository.findById(targetUserId)
					.map(u -> u.getDepartment() != null && hodDeptId.equals(u.getDepartment().getDepartmentId()))
					.orElse(false);
		}
		return false;
	}

	@Override
	public List<UserDTO> getAllUserByStatus(UserStatus status) {
		if (status == null) {
			throw new IllegalArgumentException("status is required");
		}
		List<User> users = userRepository.findByStatus(status);
		return users.stream().map(userMapper::toDto).collect(Collectors.toList());
	}
}