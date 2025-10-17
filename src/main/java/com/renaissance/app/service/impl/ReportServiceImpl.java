package com.renaissance.app.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.renaissance.app.model.Task;
import com.renaissance.app.repository.TaskRepository;
import com.renaissance.app.service.interfaces.IReportService;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@Slf4j
public class ReportServiceImpl implements IReportService {
	private final TaskRepository taskRepository;

	public ReportServiceImpl(TaskRepository taskRepository) {
		this.taskRepository = taskRepository;
	}

	@Override
	@PreAuthorize("hasAnyRole('ADMIN','HOD')")
	public Resource generateDepartmentReport(Long deptId, String format) {
		if (deptId == null) {
			throw new IllegalArgumentException("Department ID is required");
		}
		List<Task> tasks = taskRepository.findByDepartments_DepartmentId(deptId);
		return new ByteArrayResource(exportReport(tasks, format));
	}

	@Override
	@PreAuthorize("hasRole('ADMIN')")
	public Resource generateAllTasksReport(String format) {
		List<Task> tasks = taskRepository.findAll();
		return new ByteArrayResource(exportReport(tasks, format));
	}

	@Override
	@PreAuthorize("hasAnyRole('ADMIN','HOD') or #teacherId == authentication.principal.userId")
	public Resource generateTeacherReport(Long teacherId, String format) {
		if (teacherId == null) {
			throw new IllegalArgumentException("Teacher ID is required");
		}
		List<Task> tasks = taskRepository.findByAssignedUsers_UserId(teacherId);
		return new ByteArrayResource(exportReport(tasks, format));
	}

	@Override
	@PreAuthorize("hasRole('HOD')")
	public Resource generateHodReport(Long hodId, String format) {
		if (hodId == null) {
			throw new IllegalArgumentException("HOD ID is required");
		}
		List<Task> tasks = taskRepository.findByCreatedBy_UserId(hodId);
		return new ByteArrayResource(exportReport(tasks, format));
	}

	private byte[] exportReport(List<Task> tasks, String format) {
		if (format == null || format.isBlank()) {
			throw new IllegalArgumentException("Format is required");
		}
		switch (format.toUpperCase()) {
		case "CSV":
			String csv = "Task ID,Title,Status\n" + tasks.stream()
			.map(t -> String.format("%d,%s,%s", t.getTaskId(), t.getTitle(), t.getStatus().name()))
			.collect(Collectors.joining("\n"));
			return csv.getBytes();
		case "EXCEL":
			try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
				Sheet sheet = wb.createSheet("Tasks");
				Row header = sheet.createRow(0);
				header.createCell(0).setCellValue("Task ID");
				header.createCell(1).setCellValue("Title");
				header.createCell(2).setCellValue("Status");
				int rowNum = 1;
				for (Task t : tasks) {
					Row row = sheet.createRow(rowNum++);
					row.createCell(0).setCellValue(t.getTaskId());
					row.createCell(1).setCellValue(t.getTitle());
					row.createCell(2).setCellValue(t.getStatus().name());
				}
				wb.write(out);
				return out.toByteArray();
			} catch (IOException e) {
				log.error("Excel export failed: {}", e.getMessage());
				throw new RuntimeException("Excel export failed", e);
			}
		default:
			throw new IllegalArgumentException("Unsupported format: " + format);
		}
	}
}