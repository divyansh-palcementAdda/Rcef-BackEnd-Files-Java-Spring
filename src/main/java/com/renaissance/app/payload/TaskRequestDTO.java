package com.renaissance.app.payload;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.renaissance.app.model.RequestStatus;
import com.renaissance.app.model.RequestType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskRequestDTO {
	private Long requestId;
	private RequestType requestType;
	private RequestStatus status;
	private String remarks;

	private Long requestedById;
	private String requestedByName;

	private Long approvedById;
	private String approvedByName;

	private LocalDateTime requestDate;
	// Add to TaskRequestDTO.java
	private Long taskId;
	private List<TaskProofDTO> proofs;
}
