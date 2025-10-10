package com.renaissance.app.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class TaskRequestPayload {
	private Long taskId;
    private RequestType requestType; // CLOSURE, EXTENSION
    private String remarks;
}
