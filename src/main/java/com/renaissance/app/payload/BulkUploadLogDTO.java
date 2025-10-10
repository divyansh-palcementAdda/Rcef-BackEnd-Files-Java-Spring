package com.renaissance.app.payload;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)

public class BulkUploadLogDTO {
	private Long logId;
	private String fileName;
	private int totalRecords;
	private int successCount;
	private int failureCount;
	private String errorReport;

	private Long uploadedById;
	private String uploadedByName;
	private LocalDateTime uploadedAt;
}
