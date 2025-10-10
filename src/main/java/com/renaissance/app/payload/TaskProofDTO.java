package com.renaissance.app.payload;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskProofDTO {
	private Long proofId;
	private String fileUrl;
	private String fileType;
	private Long uploadedById;
	private String uploadedByName;
	private LocalDateTime uploadedAt;
}
