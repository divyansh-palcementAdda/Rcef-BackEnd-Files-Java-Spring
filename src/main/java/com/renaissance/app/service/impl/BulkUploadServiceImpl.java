package com.renaissance.app.service.impl;

import java.io.IOException;
import java.time.LocalDateTime;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.renaissance.app.model.BulkUploadLog;
import com.renaissance.app.model.User;
import com.renaissance.app.payload.BulkUploadLogDTO;
import com.renaissance.app.payload.TaskPayload;
import com.renaissance.app.repository.IBulkUploadLogRepository;
import com.renaissance.app.repository.IUserRepository;
import com.renaissance.app.service.interfaces.IBulkUploadService;
import com.renaissance.app.service.interfaces.ITaskService;

@Service
@Transactional
public class BulkUploadServiceImpl implements IBulkUploadService {

    private final IBulkUploadLogRepository bulkUploadLogRepository;
    private final IUserRepository userRepository;
    private final ITaskService taskService;
    private final ModelMapper modelMapper;

    public BulkUploadServiceImpl(IBulkUploadLogRepository bulkUploadLogRepository,
                                 IUserRepository userRepository,
                                 ITaskService taskService,
                                 ModelMapper modelMapper) {
        this.bulkUploadLogRepository = bulkUploadLogRepository;
        this.userRepository = userRepository;
        this.taskService = taskService;
        this.modelMapper = modelMapper;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public BulkUploadLogDTO uploadTasks(MultipartFile file, Long uploadedById) {
        if (file.isEmpty()) throw new IllegalArgumentException("Empty file");
        User uploader = userRepository.findById(uploadedById)
                .orElseThrow(() -> new RuntimeException("Uploader not found"));

        BulkUploadLog log = BulkUploadLog.builder()
                .fileName(file.getOriginalFilename())
                .uploadedBy(uploader)
                .uploadedAt(LocalDateTime.now())
                .build();

        int total = 0, success = 0;
        StringBuilder errors = new StringBuilder();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // skip header
                total++;
                try {
                    TaskPayload payload = parseRow(row);
                    taskService.createTask(payload);
                    success++;
                } catch (Exception e) {
                    errors.append("Row ").append(row.getRowNum() + 1).append(": ").append(e.getMessage()).append("\n");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Upload failed", e);
        }

        log.setTotalRecords(total);
        log.setSuccessCount(success);
        log.setFailureCount(total - success);
        log.setErrorReport(errors.toString());

        return modelMapper.map(bulkUploadLogRepository.save(log), BulkUploadLogDTO.class);
    }

    private TaskPayload parseRow(Row row) {
        TaskPayload payload = new TaskPayload();
        payload.setTitle(getString(row.getCell(0)));
        payload.setDescription(getString(row.getCell(1)));
        payload.setDueDate(row.getCell(2).getLocalDateTimeCellValue());
        payload.setAssignedToId((long) row.getCell(3).getNumericCellValue());
        payload.setDepartmentId((long) row.getCell(4).getNumericCellValue());
        return payload;
    }

    private String getString(Cell cell) {
        return cell != null ? cell.toString().trim() : "";
    }

    @Override
    @Transactional(readOnly = true)
    public BulkUploadLogDTO getUploadLog(Long logId) {
        if (logId == null || logId <= 0) {
            throw new IllegalArgumentException("Invalid log ID");
        }

        BulkUploadLog log = bulkUploadLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Bulk upload log not found with ID: " + logId));

        return modelMapper.map(log, BulkUploadLogDTO.class);
    }

}
