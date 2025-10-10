package com.renaissance.app.service.interfaces;

import java.util.List;

import com.renaissance.app.payload.DepartmentDTO;

public interface IDepartmentService {
	DepartmentDTO createDepartment(DepartmentDTO departmentDTO);

	DepartmentDTO updateDepartment(Long id, DepartmentDTO departmentDTO);

	void deleteDepartment(Long id);

	DepartmentDTO getDepartmentById(Long id);

	List<DepartmentDTO> getAllDepartments();


}
