package com.renaissance.app.service.interfaces;

import org.springframework.web.multipart.MultipartFile;

import com.renaissance.app.payload.BulkUploadLogDTO;

public interface IBulkUploadService {

	BulkUploadLogDTO uploadTasks(MultipartFile file, Long uploadedById);

	BulkUploadLogDTO getUploadLog(Long logId);
}
