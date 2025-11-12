package com.renaissance.app.service.interfaces;

import java.util.List;

import com.renaissance.app.exception.AccessDeniedException;
import com.renaissance.app.exception.ResourcesNotFoundException;
import com.renaissance.app.model.Role;
import com.renaissance.app.model.UserStatus;
import com.renaissance.app.payload.UserDTO;
import com.renaissance.app.payload.UserRequest;

public interface IUserService {

	UserDTO updateUser(Long userId, UserRequest request) throws AccessDeniedException, ResourcesNotFoundException;

	void deleteUser(Long userId) throws AccessDeniedException, ResourcesNotFoundException;

	UserDTO getUserById(Long userId) throws AccessDeniedException, ResourcesNotFoundException;

	List<UserDTO> getAllUsers() throws AccessDeniedException;
	
	List<UserDTO> getAllUserByStatus(UserStatus status);

	void validateUser(String email);
	
	UserDTO activeUserById(Long userId);
	
    List<UserDTO> getUsersByDepartment(Long departmentId) throws AccessDeniedException;
	
	List<UserDTO> getAllUserByRole(Role role) throws AccessDeniedException, ResourcesNotFoundException;

	List<UserDTO> getUsersByIds(List<Long> ids) throws ResourcesNotFoundException;
}
