//package com.renaissance.app.service.impl;
//
//import java.io.IOException;
//import java.time.LocalDateTime;
//import java.util.Collections;
//import java.util.Objects;
//import org.apache.poi.ss.usermodel.*;
//import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//import org.modelmapper.ModelMapper;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.multipart.MultipartFile;
//import com.renaissance.app.model.BulkUploadLog;
//import com.renaissance.app.model.User;
//import com.renaissance.app.payload.BulkUploadLogDTO;
//import com.renaissance.app.payload.TaskPayload;
//import com.renaissance.app.repository.IBulkUploadLogRepository;
//import com.renaissance.app.repository.IUserRepository;
//import com.renaissance.app.service.interfaces.IBulkUploadService;
//import com.renaissance.app.service.interfaces.ITaskService;
//import lombok.extern.slf4j.Slf4j;
//
//@Service
//@Transactional
//@Slf4j
//public class BulkUploadServiceImpl implements IBulkUploadService {
//	private final IBulkUploadLogRepository bulkUploadLogRepository;
//	private final IUserRepository userRepository;
//	private final ITaskService taskService;
//	private final ModelMapper modelMapper;
//
//	public BulkUploadServiceImpl(IBulkUploadLogRepository bulkUploadLogRepository, IUserRepository userRepository,
//			ITaskService taskService, ModelMapper modelMapper) {
//		this.bulkUploadLogRepository = bulkUploadLogRepository;
//		this.userRepository = userRepository;
//		this.taskService = taskService;
//		this.modelMapper = modelMapper;
//	}
//
//	@Override
//	@PreAuthorize("hasRole('ADMIN')")
//	public BulkUploadLogDTO uploadTasks(MultipartFile file, Long uploadedById) {
//		if (file == null || file.isEmpty()) {
//			throw new IllegalArgumentException("Uploaded file cannot be empty");
//		}
//		if (uploadedById == null) {
//			throw new IllegalArgumentException("Uploader ID is required");
//		}
//		User uploader = userRepository.findById(uploadedById)
//				.orElseThrow(() -> new RuntimeException("Uploader not found with ID: " + uploadedById));
//		BulkUploadLog log = BulkUploadLog.builder().fileName(file.getOriginalFilename()).uploadedBy(uploader)
//				.uploadedAt(LocalDateTime.now()).build();
//		int total = 0, success = 0;
//		StringBuilder errors = new StringBuilder();
//		try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
//			Sheet sheet = workbook.getSheetAt(0);
//			if (sheet == null) {
//				throw new IllegalArgumentException("Excel file contains no sheets");
//			}
//			for (Row row : sheet) {
//				if (row.getRowNum() == 0)
//					continue; // Skip header
//				total++;
//				try {
//					TaskPayload payload = parseRow(row);
//					taskService.createTask(payload);
//					success++;
//				} catch (Exception e) {
////					log.warn("Row {} failed: {}", row.getRowNum() + 1, e.getMessage());
//					errors.append("Row ").append(row.getRowNum() + 1).append(": ").append(e.getMessage()).append("\n");
//				}
//			}
//		} catch (IOException e) {
////			log.error("Failed to read Excel file: {}", e.getMessage());
//			throw new RuntimeException("Failed to read Excel file", e);
//		}
//		log.setTotalRecords(total);
//		log.setSuccessCount(success);
//		log.setFailureCount(total - success);
//		log.setErrorReport(errors.length() > 0 ? errors.toString() : "No errors");
//		BulkUploadLog savedLog = bulkUploadLogRepository.save(log);
////		log.info("Bulk upload completed for file {}: {} successes, {} failures", file.getOriginalFilename(), success,
////				total - success);
//		return modelMapper.map(savedLog, BulkUploadLogDTO.class);
//	}
//
//	private TaskPayload parseRow(Row row) {
//		TaskPayload payload = new TaskPayload();
//		payload.setTitle(getString(row.getCell(0)));
//		payload.setDescription(getString(row.getCell(1)));
//		payload.setDueDate(getLocalDateTime(row.getCell(2)));
//		Long assignedToId = getLong(row.getCell(3), "AssignedTo ID");
//		payload.setAssignedToId(assignedToId);
//		payload.setAssignedToIds(assignedToId != null ? Collections.singleton(assignedToId) : Collections.emptySet());
//		Long departmentId = getLong(row.getCell(4), "Department ID");
//		payload.setDepartmentId(departmentId);
//		payload.setDepartmentIds(departmentId != null ? Collections.singleton(departmentId) : Collections.emptySet());
//		return payload;
//	}
//
//	private String getString(Cell cell) {
//		if (cell == null || cell.getCellType() == CellType.BLANK) {
//			return "";
//		}
//		try {
//			cell.setCellType(CellType.STRING); // Handle numeric or formula cells
//			return cell.getStringCellValue().trim();
//		} catch (Exception e) {
//			throw new IllegalArgumentException("Invalid string value in cell");
//		}
//	}
//
//	private LocalDateTime getLocalDateTime(Cell cell) {
//		if (cell == null || cell.getCellType() == CellType.BLANK) {
//			return LocalDateTime.now().plusDays(7);
//		}
//		try {
//			return cell.getLocalDateTimeCellValue();
//		} catch (Exception e) {
//			throw new IllegalArgumentException("Invalid date format in cell: " + e.getMessage());
//		}
//	}
//
//	private Long getLong(Cell cell, String fieldName) {
//		if (cell == null || cell.getCellType() == CellType.BLANK) {
//			throw new IllegalArgumentException(fieldName + " cannot be empty");
//		}
//		try {
//			if (cell.getCellType() != CellType.NUMERIC) {
//				cell.setCellType(CellType.NUMERIC); // Attempt to convert
//			}
//			double value = cell.getNumericCellValue();
//			if (value % 1 != 0) {
//				throw new IllegalArgumentException(fieldName + " must be an integer");
//			}
//			return (long) value;
//		} catch (Exception e) {
//			throw new IllegalArgumentException(fieldName + " must be a valid numeric ID: " + e.getMessage());
//		}
//	}
//
//	@Override
//	@Transactional(readOnly = true)
//	public BulkUploadLogDTO getUploadLog(Long logId) {
//		Objects.requireNonNull(logId, "Log ID is required");
//		BulkUploadLog log = bulkUploadLogRepository.findById(logId)
//				.orElseThrow(() -> new RuntimeException("Bulk upload log not found with ID: " + logId));
//		return modelMapper.map(log, BulkUploadLogDTO.class);
//	}
//}