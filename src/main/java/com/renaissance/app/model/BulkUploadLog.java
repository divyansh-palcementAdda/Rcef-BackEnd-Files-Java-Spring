package com.renaissance.app.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bulk_upload_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkUploadLog {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long logId;

	@NotBlank(message = "File name cannot be blank")
	@Size(min = 1, max = 255, message = "File name must be between 1 and 255 characters")
	private String fileName;

	@Min(value = 0, message = "Total records cannot be negative")
	private int totalRecords;

	@Min(value = 0, message = "Success count cannot be negative")
	private int successCount;

	@Min(value = 0, message = "Failure count cannot be negative")
	private int failureCount;

	@Lob
	@Size(max = 10000, message = "Error report cannot exceed 10000 characters")
	private String errorReport; // details of failed rows

	@NotNull(message = "Uploaded by user cannot be null")
	@ManyToOne
	@JoinColumn(name = "uploaded_by")
	private User uploadedBy;

	@NotNull(message = "Uploaded at date cannot be null")
	@PastOrPresent(message = "Uploaded at date must be in the past or present")
	private LocalDateTime uploadedAt;
}