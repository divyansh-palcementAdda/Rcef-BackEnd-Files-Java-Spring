package com.renaissance.app.service.interfaces;

import java.util.List;

import com.renaissance.app.exception.AccessDeniedException;
import com.renaissance.app.exception.BadRequestException;
import com.renaissance.app.exception.ResourcesNotFoundException;
import com.renaissance.app.model.Role;
import com.renaissance.app.model.UserStatus;
import com.renaissance.app.payload.UserDTO;
import com.renaissance.app.payload.UserRequest;

public interface IUserService {

	UserDTO updateUser(Long userId, UserRequest request) throws AccessDeniedException, ResourcesNotFoundException, BadRequestException;

	void deleteUser(Long userId) throws AccessDeniedException, ResourcesNotFoundException, BadRequestException;

	UserDTO getUserById(Long userId) throws AccessDeniedException, ResourcesNotFoundException;

	List<UserDTO> getAllUsers() throws AccessDeniedException, ResourcesNotFoundException;
	
	List<UserDTO> getAllUserByStatus(UserStatus status) throws BadRequestException;
	
	void activeUserById(Long userId) throws ResourcesNotFoundException;
	
    List<UserDTO> getUsersByDepartment(Long departmentId) throws AccessDeniedException, ResourcesNotFoundException;
	
	List<UserDTO> getAllUserByRole(Role role) throws AccessDeniedException, ResourcesNotFoundException;

	List<UserDTO> getUsersByIds(List<Long> ids) throws ResourcesNotFoundException, BadRequestException;
}
