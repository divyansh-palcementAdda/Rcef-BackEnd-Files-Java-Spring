package com.renaissance.app.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.renaissance.app.exception.AccessDeniedException;
import com.renaissance.app.model.Role;
import com.renaissance.app.model.UserStatus;
import com.renaissance.app.payload.UserDTO;
import com.renaissance.app.payload.UserRequest;
import com.renaissance.app.service.interfaces.IUserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = { "http://localhost:4200" }, allowCredentials = "true")
@RequiredArgsConstructor
@Slf4j
public class UserController {

	private final IUserService userService;

	@GetMapping
	public ResponseEntity<?> getAllUsers() {
		try {
			List<UserDTO> users = userService.getAllUsers();
			return ResponseEntity.ok(users);
		} catch (AccessDeniedException ex) {
			log.warn("Access denied to get all users: {}", ex.getMessage());
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));
		} catch (Exception ex) {
			log.error("Failed to get users", ex);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "Failed to fetch users"));
		}
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> getUser(@PathVariable("id") Long id) {
		try {
			UserDTO dto = userService.getUserById(id);
			return ResponseEntity.ok(dto);
		} catch (AccessDeniedException ex) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));
		} catch (RuntimeException ex) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
		} catch (Exception ex) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "Error fetching user"));
		}
	}

	@PutMapping("/{id}")
	public ResponseEntity<?> updateUser(@PathVariable("id") Long id, @RequestBody UserRequest request) {
		try {
			UserDTO updated = userService.updateUser(id, request);
			return ResponseEntity.ok(updated);
		} catch (AccessDeniedException ex) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
		} catch (RuntimeException ex) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
		} catch (Exception ex) {
			log.error("Error updating user", ex);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "Failed to update user"));
		}
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> deleteUser(@PathVariable("id") Long id) {
		try {
			userService.deleteUser(id);
			return ResponseEntity.ok(Map.of("message", "User marked inactive"));
		} catch (SecurityException ex) {
			// e.g. trying to delete self or admin
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
		} catch (AccessDeniedException ex) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));
		} catch (RuntimeException ex) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
		} catch (Exception ex) {
			log.error("Error deleting user", ex);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "Failed to delete user"));
		}
	}

	@PutMapping("/{userId}/toggle-status")
	public ResponseEntity<?> activeUserById(@PathVariable("userId") Long userId){
		try {
			userService.activeUserById(userId);
			return ResponseEntity.ok(Map.of("message", "User marked ACTIVE"));
		} catch (RuntimeException ex) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
		} catch (Exception ex) {
			log.error("Error deleting user", ex);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "Failed to ACTIVE user"));
		}
	
		
	}
	
	@GetMapping("/role/{role}")
	public ResponseEntity<?> getUsersByRole(@PathVariable("role") String roleStr) {
		try {
			Role role = Role.valueOf(roleStr.toUpperCase());
			List<UserDTO> users = userService.getAllUserByRole(role);
			return ResponseEntity.ok(users);
		} catch (IllegalArgumentException ex) {
			return ResponseEntity.badRequest().body(Map.of("message", "Invalid role"));
		} catch (AccessDeniedException ex) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));
		} catch (Exception ex) {
			log.error("Error fetching users by role", ex);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "Failed to fetch users"));
		}
	}

	@GetMapping("/department/{deptId}")
	public ResponseEntity<?> getUsersByDepartment(@PathVariable("deptId") Long deptId) {
		try {
			List<UserDTO> users = userService.getUsersByDepartment(deptId);
			return ResponseEntity.ok(users);
		} catch (AccessDeniedException ex) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));
		} catch (RuntimeException ex) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
		} catch (Exception ex) {
			log.error("Error fetching users by department", ex);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "Failed to fetch users"));
		}
	}

	@GetMapping("status/{status}")
	public ResponseEntity<?> getUsersByStatus(@PathVariable("status") UserStatus status) {
		try {
			List<UserDTO> users = userService.getAllUserByStatus(status);
			return ResponseEntity.ok(users);
		} catch (RuntimeException ex) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
		} catch (Exception ex) {
			log.error("Error fetching users by Status", ex);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "Failed to fetch users"));
		}
	}

}
