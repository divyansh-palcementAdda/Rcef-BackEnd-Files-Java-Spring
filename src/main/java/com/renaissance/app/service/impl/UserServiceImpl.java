package com.renaissance.app.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renaissance.app.exception.AccessDeniedException;
import com.renaissance.app.exception.BadRequestException;
import com.renaissance.app.exception.ResourcesNotFoundException;
import com.renaissance.app.mapper.UserMapper;
import com.renaissance.app.model.Department;
import com.renaissance.app.model.Role;
import com.renaissance.app.model.User;
import com.renaissance.app.model.UserStatus;
import com.renaissance.app.payload.NotificationEvent;
import com.renaissance.app.payload.UserDTO;
import com.renaissance.app.payload.UserRequest;
import com.renaissance.app.repository.DepartmentRepository;
import com.renaissance.app.repository.IUserRepository;
import com.renaissance.app.repository.TaskRepository;
import com.renaissance.app.security.UserSecurityUtil;
import com.renaissance.app.service.interfaces.IEmailService;
import com.renaissance.app.service.interfaces.IUserService;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements IUserService {

    private final IUserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserSecurityUtil securityUtil;
    private final UserMapper userMapper;
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final IEmailService emailService;
	private final TaskRepository taskRepository;

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','HOD')")
    public UserDTO updateUser(Long userId, UserRequest request) throws AccessDeniedException, BadRequestException, ResourcesNotFoundException {
        if (userId == null) throw new BadRequestException("User ID is required");
        if (request == null) throw new BadRequestException("Update request is required");

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourcesNotFoundException("User not found: " + userId));

        User updater = getCurrentUser();
        if (!canAccessUser(targetUser, updater)) {
            throw new AccessDeniedException("Not authorized to update this user");
        }

        if (request.getUsername() != null && !request.getUsername().equals(targetUser.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new BadRequestException("Username already exists");
            }
            targetUser.setUsername(request.getUsername());
        }

        if (request.getEmail() != null && !request.getEmail().equals(targetUser.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email already exists");
            }
            targetUser.setEmail(request.getEmail());
            targetUser.setEmailVerified(false); // Require re-verification
        }

        if (request.getPassword() != null) {
            targetUser.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRole() != null && request.getRole() != targetUser.getRole()) {
            if (updater.getRole() != Role.ADMIN) {
                throw new AccessDeniedException("Only Admin can change roles");
            }
            targetUser.setRole(request.getRole());
        }

        if (request.getDepartmentIds() != null && !request.getDepartmentIds().isEmpty()) {
            Set<Department> newDepts = request.getDepartmentIds().stream()
                .map(deptId -> {
					try {
						return departmentRepository.findById(deptId)
						    .orElseThrow(() -> new ResourcesNotFoundException("Department not found with ID: " + deptId));
					} catch (ResourcesNotFoundException e) {
						e.printStackTrace();
					}
					return null;
				})
                .collect(Collectors.toSet());

            // Convert Set → List if User.setDepartments(List<Department>) is required
            targetUser.setDepartments(new ArrayList<>(newDepts));
        } else if (request.getDepartmentIds() != null) {
            // Explicitly clear departments if empty list is sent
            targetUser.setDepartments(new ArrayList<>());
        }

        targetUser.setUpdatedAt(LocalDateTime.now());
        User updated = userRepository.save(targetUser);

        publishUserEvent("USER_UPDATED", updated, "User profile updated");
        sendUserUpdateEmail(updated);

        return userMapper.toDto(updated);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','HOD')")
    public void deleteUser(Long userId) throws AccessDeniedException, ResourcesNotFoundException, BadRequestException {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourcesNotFoundException("User not found: " + userId));

        User deleter = getCurrentUser();
        if (!canAccessUser(targetUser, deleter)) {
            throw new AccessDeniedException("Not authorized to delete this user");
        }

        if (taskRepository.existsByAssignedUsersContaining(targetUser)) {
            throw new BadRequestException("Cannot delete user with active tasks");
        }

        targetUser.setStatus(UserStatus.INACTIVE);
        userRepository.save(targetUser);

        publishUserEvent("USER_DELETED", targetUser, "User marked inactive");
        sendUserStatusEmail(targetUser, "Your account has been deactivated");
    }

    @Override
    public UserDTO getUserById(Long userId) throws AccessDeniedException, ResourcesNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourcesNotFoundException("User not found: " + userId));

        User viewer = getCurrentUser();
        if (!canAccessUser(user, viewer)) {
            throw new AccessDeniedException("Not authorized to view this user");
        }

        return userMapper.toDto(user);
    }

    @Override
    public List<UserDTO> getAllUsers() throws AccessDeniedException, ResourcesNotFoundException {
        User current = getCurrentUser();
        if (!current.getRole().equals(Role.ADMIN)) {
            throw new AccessDeniedException("Only Admin can view all users");
        }
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDTO> getUsersByIds(List<Long> ids) throws ResourcesNotFoundException, BadRequestException {
        if (ids == null || ids.isEmpty()) {
            throw new BadRequestException("User IDs are required");
        }
        List<User> users = userRepository.findAllById(ids);
        if (users.isEmpty()) {
            throw new ResourcesNotFoundException("No users found for given IDs");
        }
        return users.stream().map(userMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public List<UserDTO> getAllUserByRole(Role role) throws AccessDeniedException, ResourcesNotFoundException {
        User current = getCurrentUser();
        if (!current.getRole().equals(Role.ADMIN) && !current.getRole().equals(Role.HOD)) {
            throw new AccessDeniedException("Not authorized to view users by role");
        }
        return userRepository.findByRole(role).stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDTO> getUsersByDepartment(Long departmentId) throws AccessDeniedException, ResourcesNotFoundException {
        Department dept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourcesNotFoundException("Department not found: " + departmentId));

        User current = getCurrentUser();
        if (current.getRole() == Role.HOD && !current.getDepartments().contains(dept)) {
            throw new AccessDeniedException("HOD can only view users in their own department");
        } else if (!current.getRole().equals(Role.ADMIN) && !current.getRole().equals(Role.HOD)) {
            throw new AccessDeniedException("Not authorized");
        }

        return userRepository.findByDepartmentsContaining(dept).stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDTO> getAllUserByStatus(UserStatus status) throws BadRequestException {
        if (status == null) throw new BadRequestException("Status is required");
        return userRepository.findByStatus(status).stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void activeUserById(Long userId) throws ResourcesNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourcesNotFoundException("User not found: " + userId));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        publishUserEvent("USER_ACTIVATED", user, "User activated");
        sendUserStatusEmail(user, "Your account has been activated");
    }

    private User getCurrentUser() throws ResourcesNotFoundException {
        Long currentId = securityUtil.getCurrentUserId();
        return userRepository.findById(currentId)
                .orElseThrow(() -> new ResourcesNotFoundException("Current user not found"));
    }

    private boolean canAccessUser(User target, User current) {
        if (current.getUserId().equals(target.getUserId()) || current.getRole() == Role.ADMIN) return true;

        if (current.getRole() == Role.HOD) {
            return target.getDepartments().stream()
                    .anyMatch(dept -> current.getDepartments().contains(dept));
        }
        return false;
    }

    private void publishUserEvent(String eventType, User user, String message) {
        NotificationEvent event = new NotificationEvent();
        event.setType(eventType);
        event.setId(user.getUserId());
        event.setMessage(message);
        event.setTimestamp(LocalDateTime.now());

        kafkaTemplate.send("user.events", user.getUserId().toString(), event); // Assume user.events topic
    }

    private void sendUserUpdateEmail(User user) {
        try {
            emailService.sendTaskUpdateEmail(user.getEmail(), "User Profile", "Your profile has been updated");
        } catch (MessagingException e) {
            log.error("Failed to send update email to {}", user.getEmail(), e);
        }
    }

    private void sendUserStatusEmail(User user, String message) {
        try {
            emailService.sendTaskUpdateEmail(user.getEmail(), "Account Status", message);
        } catch (MessagingException e) {
            log.error("Failed to send status email to {}", user.getEmail(), e);
        }
    }

	
}