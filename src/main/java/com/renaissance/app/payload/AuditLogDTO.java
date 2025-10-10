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
public class AuditLogDTO {
	private Long logId;
	private String action;
	private String entity;
	private Long entityId;
	private Long userId;
	private String username;
	private LocalDateTime timestamp;
}
