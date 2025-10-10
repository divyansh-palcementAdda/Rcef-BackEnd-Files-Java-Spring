package com.renaissance.app.service.interfaces;

import java.util.List;

import com.renaissance.app.payload.TaskRequestDTO;
import com.renaissance.app.payload.TaskRequestPayload;

public interface ITaskRequestService {
	TaskRequestDTO createRequest(TaskRequestPayload payload, Long requestedById);

	TaskRequestDTO approveRequest(Long requestId, Long approverId);

	TaskRequestDTO rejectRequest(Long requestId, Long approverId, String reason);

	List<TaskRequestDTO> getRequestsForTask(Long taskId);
}
