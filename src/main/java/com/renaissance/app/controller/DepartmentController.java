package com.renaissance.app.controller;

import com.renaissance.app.exception.BadRequestException;
import com.renaissance.app.exception.ResourcesNotFoundException;
import com.renaissance.app.payload.DepartmentDTO;
import com.renaissance.app.service.interfaces.IDepartmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@Slf4j
public class DepartmentController {

    private final IDepartmentService departmentService;

    // Create Department
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<?> createDepartment(@Valid @RequestBody DepartmentDTO departmentDTO) {
        try {
            DepartmentDTO created = departmentService.createDepartment(departmentDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (BadRequestException ex) {
            log.warn("Failed to create department: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Unexpected error while creating department", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to create department"));
        }
    }

    // Update Department
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<?> updateDepartment(@PathVariable Long id,
                                              @Valid @RequestBody DepartmentDTO departmentDTO) {
        try {
            DepartmentDTO updated = departmentService.updateDepartment(id, departmentDTO);
            return ResponseEntity.ok(updated);
        } catch (ResourcesNotFoundException | BadRequestException ex) {
            log.warn("Failed to update department {}: {}", id, ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Unexpected error while updating department {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to update department"));
        }
    }

    // Delete Department
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<?> deleteDepartment(@PathVariable Long id) {
        try {
            departmentService.deleteDepartment(id);
            return ResponseEntity.noContent().build();
        } catch (ResourcesNotFoundException ex) {
            log.warn("Department not found for deletion {}: {}", id, ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Unexpected error while deleting department {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to delete department"));
        }
    }

    // Get Department by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getDepartmentById(@PathVariable Long id) {
        try {
            DepartmentDTO dept = departmentService.getDepartmentById(id);
            return ResponseEntity.ok(dept);
        } catch (ResourcesNotFoundException ex) {
            log.warn("Department not found {}: {}", id, ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Unexpected error while fetching department {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to fetch department"));
        }
    }

    // Get All Departments
    @GetMapping
    public ResponseEntity<List<DepartmentDTO>> getAllDepartments() {
        List<DepartmentDTO> depts = departmentService.getAllDepartments();
        return ResponseEntity.ok(depts);
    }
}
