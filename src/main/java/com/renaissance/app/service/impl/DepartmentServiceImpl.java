package com.renaissance.app.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renaissance.app.exception.BadRequestException;
import com.renaissance.app.exception.ResourcesNotFoundException;
import com.renaissance.app.mapper.DepartmentMapper;
import com.renaissance.app.model.Department;
import com.renaissance.app.payload.DepartmentDTO;
import com.renaissance.app.repository.DepartmentRepository;
import com.renaissance.app.service.interfaces.IDepartmentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DepartmentServiceImpl implements IDepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;

    // ================= CREATE ================= //
    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public DepartmentDTO createDepartment(DepartmentDTO dto) throws BadRequestException {
        validateDepartmentDTO(dto);

        if (departmentRepository.existsByName(dto.getName().trim())) {
            throw new BadRequestException("Department with this name already exists");
        }

        Department dept = departmentMapper.toEntity(dto);
        dept.setCreatedAt(LocalDateTime.now());
        Department saved = departmentRepository.save(dept);
        log.info("✅ Department created successfully: {}", saved.getName());
        return departmentMapper.toDto(saved);
    }

    // ================= UPDATE ================= //
    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public DepartmentDTO updateDepartment(Long id, DepartmentDTO dto) throws ResourcesNotFoundException, BadRequestException {
        if (id == null) throw new BadRequestException("Department ID is required");

        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourcesNotFoundException("Department not found with id: " + id));

        if (dto.getName() != null && !dto.getName().isBlank() && !dept.getName().equalsIgnoreCase(dto.getName())) {
            if (departmentRepository.existsByName(dto.getName().trim())) {
                throw new BadRequestException("Department name already in use");
            }
            dept.setName(dto.getName().trim());
        }

        if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
            dept.setDescription(dto.getDescription().trim());
        }

//        dept.setUpdatedAt(LocalDateTime.now());
        Department updated = departmentRepository.save(dept);
        log.info("✏️ Department updated successfully: {}", updated.getName());
        return departmentMapper.toDto(updated);
    }

    // ================= DELETE ================= //
    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public void deleteDepartment(Long id) throws ResourcesNotFoundException {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourcesNotFoundException("Department not found with id: " + id));
        departmentRepository.delete(dept);
        log.info("🗑️ Department deleted successfully with id: {}", id);
    }

    // ================= READ BY ID ================= //
    @Override
    @Transactional(readOnly = true)
    public DepartmentDTO getDepartmentById(Long id) throws ResourcesNotFoundException {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourcesNotFoundException("Department not found with id: " + id));
        return departmentMapper.toDto(dept);
    }

    // ================= READ ALL ================= //
    @Override
    @Transactional(readOnly = true)
    public List<DepartmentDTO> getAllDepartments() {
        return departmentRepository.findAll()
                .stream()
                .map(departmentMapper::toDto)
                .collect(Collectors.toList());
    }

    // =================== VALIDATION =================== //
    private void validateDepartmentDTO(DepartmentDTO dto) throws BadRequestException {
        if (dto == null) throw new BadRequestException("Department details are required");
        if (dto.getName() == null || dto.getName().isBlank()) throw new BadRequestException("Department name is required");
        if (dto.getDescription() == null || dto.getDescription().isBlank()) throw new BadRequestException("Department description is required");
    }
}
