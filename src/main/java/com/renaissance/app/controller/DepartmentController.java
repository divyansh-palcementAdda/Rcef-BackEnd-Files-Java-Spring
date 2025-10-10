package com.renaissance.app.controller;

import com.renaissance.app.payload.DepartmentDTO;
import com.renaissance.app.service.interfaces.IDepartmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
//@CrossOrigin(origins = {"http://localhost:4200"}, allowCredentials = "true")
public class DepartmentController {

	private final IDepartmentService departmentService;

	public DepartmentController(IDepartmentService departmentService) {
		this.departmentService = departmentService;
	}

	// ✅ Create Department
	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<DepartmentDTO> createDepartment(@Valid @RequestBody DepartmentDTO departmentDTO) {
		System.err.println("createDepartment From Controller");
		DepartmentDTO created = departmentService.createDepartment(departmentDTO);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	// ✅ Update Department (Partial update supported)
	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<DepartmentDTO> updateDepartment(@PathVariable Long id,
			@Valid @RequestBody DepartmentDTO departmentDTO) {
		DepartmentDTO updated = departmentService.updateDepartment(id, departmentDTO);
		return ResponseEntity.ok(updated);
	}

	// ✅ Delete Department
	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
		departmentService.deleteDepartment(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}")
	// ✅ Get Department by ID
	public ResponseEntity<DepartmentDTO> getDepartmentById(@PathVariable Long id) {
		DepartmentDTO dept = departmentService.getDepartmentById(id);
		return ResponseEntity.ok(dept);
	}

	// ✅ Get All Departments
	@GetMapping
	public ResponseEntity<List<DepartmentDTO>> getAllDepartments() {
		List<DepartmentDTO> depts = departmentService.getAllDepartments();
		return ResponseEntity.ok(depts);
	}
}
