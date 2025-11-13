package com.renaissance.app.service.interfaces;

import java.io.IOException;
import java.util.List;

import com.renaissance.app.exception.BadRequestException;
import com.renaissance.app.exception.UnauthorizedException;
import com.renaissance.app.payload.ApproveRequestPayload;
import com.renaissance.app.payload.TaskRequestDTO;
import com.renaissance.app.payload.TaskRequestMultipartPayload;

public interface ITaskRequestService {

	TaskRequestDTO approveRequest(ApproveRequestPayload payload, Long approverId) throws BadRequestException;
	
	TaskRequestDTO rejectRequest(Long requestId, Long approverId, String reason) throws BadRequestException;

	List<TaskRequestDTO> getRequestsForTask(Long taskId);


	TaskRequestDTO createRequestWithProofs(Long taskId, TaskRequestMultipartPayload payload, Long requesterId)
			throws UnauthorizedException, IOException, BadRequestException;
}
