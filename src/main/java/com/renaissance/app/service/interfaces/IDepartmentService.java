package com.renaissance.app.service.interfaces;

import java.util.List;

import com.renaissance.app.exception.BadRequestException;
import com.renaissance.app.exception.ResourcesNotFoundException;
import com.renaissance.app.payload.DepartmentDTO;

public interface IDepartmentService {
	DepartmentDTO createDepartment(DepartmentDTO departmentDTO) throws BadRequestException;

	DepartmentDTO updateDepartment(Long id, DepartmentDTO departmentDTO) throws ResourcesNotFoundException, BadRequestException;

	void deleteDepartment(Long id) throws ResourcesNotFoundException;

	DepartmentDTO getDepartmentById(Long id) throws ResourcesNotFoundException;

	List<DepartmentDTO> getAllDepartments();


}
