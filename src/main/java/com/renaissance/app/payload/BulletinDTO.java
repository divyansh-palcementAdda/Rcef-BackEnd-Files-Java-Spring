package com.renaissance.app.payload;

import java.time.LocalDateTime;

import com.renaissance.app.model.Severity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulletinDTO {
	private Long bulletinId;
	private String message;
	private Severity severity;
	private Long taskId;
	private String taskTitle;
	private LocalDateTime createdAt;
}
