package com.renaissance.app.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renaissance.app.model.Department;
import com.renaissance.app.payload.DepartmentDTO;
import com.renaissance.app.repository.DepartmentRepository;
import com.renaissance.app.service.interfaces.IDepartmentService;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class DepartmentServiceImpl implements IDepartmentService {

	private final DepartmentRepository departmentRepository;
	private final ModelMapper modelMapper;

	public DepartmentServiceImpl(DepartmentRepository departmentRepository, ModelMapper modelMapper) {
		this.departmentRepository = departmentRepository;
		this.modelMapper = modelMapper;
	}

	@Override
	@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
	public DepartmentDTO createDepartment(DepartmentDTO departmentDTO) {
		System.err.println("createDepartment");
		if (departmentDTO == null) {
			throw new IllegalArgumentException("Department details are required");
		}
		String name = departmentDTO.getName();
		String description = departmentDTO.getDescription();

		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Department name is required");
		}
		if (description == null || description.isBlank()) {
			throw new IllegalArgumentException("Department description is required");
		}
		if (departmentRepository.existsByName(name.trim())) {
			throw new IllegalArgumentException("Department with this name already exists");
		}

		Department dept = modelMapper.map(departmentDTO, Department.class);
		dept.setCreatedAt(LocalDateTime.now());
		Department saved = departmentRepository.save(dept);

		log.info("✅ Department created: {}", saved.getName());
		return modelMapper.map(saved, DepartmentDTO.class);
	}

	@Override
	@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
	public DepartmentDTO updateDepartment(Long id, DepartmentDTO departmentDTO) {
		Department dept = departmentRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + id));

		// ✅ Update name if provided and valid
		if (departmentDTO.getName() != null && !departmentDTO.getName().isBlank()) {
			String newName = departmentDTO.getName().trim();
			if (!dept.getName().equalsIgnoreCase(newName) && departmentRepository.existsByName(newName)) {
				throw new IllegalArgumentException("Department name already in use");
			}
			dept.setName(newName);
		}

		// ✅ Update description if provided
		if (departmentDTO.getDescription() != null && !departmentDTO.getDescription().isBlank()) {
			dept.setDescription(departmentDTO.getDescription().trim());
		}

		Department updated = departmentRepository.save(dept);
		log.info("✏️ Department updated: {}", updated.getName());
		return modelMapper.map(updated, DepartmentDTO.class);
	}

	@Override
	@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
	public void deleteDepartment(Long id) {
		Department dept = departmentRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + id));
		departmentRepository.delete(dept);
		log.info("🗑️ Department deleted with id: {}", id);
	}

	@Override
	public DepartmentDTO getDepartmentById(Long id) {
		Department dept = departmentRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + id));
		return modelMapper.map(dept, DepartmentDTO.class);
	}

	@Override
	public List<DepartmentDTO> getAllDepartments() {
		return departmentRepository.findAll().stream().map(d -> modelMapper.map(d, DepartmentDTO.class))
				.collect(Collectors.toList());
	}
}
