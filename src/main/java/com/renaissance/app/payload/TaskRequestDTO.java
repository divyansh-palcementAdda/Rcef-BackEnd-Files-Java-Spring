package com.renaissance.app.payload;

import java.time.LocalDateTime;

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
}
