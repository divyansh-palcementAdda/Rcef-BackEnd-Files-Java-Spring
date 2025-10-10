package com.renaissance.app.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renaissance.app.model.Task;
import com.renaissance.app.repository.TaskRepository;
import com.renaissance.app.service.interfaces.IReportService;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements IReportService {

    private final TaskRepository taskRepository;

    public ReportServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','HOD')")
    public Resource generateDepartmentReport(Long deptId, String format) {
        List<Task> tasks = taskRepository.findByDepartment_DepartmentId(deptId);
        return new ByteArrayResource(exportReport(tasks, format));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Resource generateAllTasksReport(String format) {
        List<Task> tasks = taskRepository.findAll();
        return new ByteArrayResource(exportReport(tasks, format));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or #teacherId == principal.userId")
    public Resource generateTeacherReport(Long teacherId, String format) {
        List<Task> tasks = taskRepository.findByAssignedTo_UserId(teacherId);
        return new ByteArrayResource(exportReport(tasks, format));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','HOD')")
    public Resource generateHodReport(Long hodId, String format) {
        List<Task> tasks = taskRepository.findByCreatedBy_UserId(hodId);
        return new ByteArrayResource(exportReport(tasks, format));
    }

    // ---------------- Helper ----------------
    private byte[] exportReport(List<Task> tasks, String format) {
        return switch (format.toUpperCase()) {
            case "CSV" -> tasks.stream()
                    .map(t -> t.getTaskId() + "," + t.getTitle() + "," + t.getStatus())
                    .collect(Collectors.joining("\n"))
                    .getBytes();

            case "EXCEL" -> {
                try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    Sheet sheet = wb.createSheet("Tasks");
                    int rowNum = 0;
                    for (Task t : tasks) {
                        Row row = sheet.createRow(rowNum++);
                        row.createCell(0).setCellValue(t.getTaskId());
                        row.createCell(1).setCellValue(t.getTitle());
                        row.createCell(2).setCellValue(t.getStatus().name());
                    }
                    wb.write(out);
                    yield out.toByteArray();
                } catch (IOException e) {
                    throw new RuntimeException("Excel export failed", e);
                }
            }

            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        };
    }
}
