package com.renaissance.app.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renaissance.app.model.RequestStatus;
import com.renaissance.app.model.Task;
import com.renaissance.app.model.TaskRequest;
import com.renaissance.app.model.User;
import com.renaissance.app.payload.TaskRequestDTO;
import com.renaissance.app.payload.TaskRequestPayload;
import com.renaissance.app.repository.IUserRepository;
import com.renaissance.app.repository.TaskRepository;
import com.renaissance.app.repository.TaskRequestRepository;
import com.renaissance.app.service.interfaces.ITaskRequestService;

@Service
@Transactional
public class TaskRequestServiceImpl implements ITaskRequestService {

    private final TaskRequestRepository taskRequestRepository;
    private final IUserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ModelMapper modelMapper;

    public TaskRequestServiceImpl(TaskRequestRepository taskRequestRepository,
                                  IUserRepository userRepository,
                                  TaskRepository taskRepository,
                                  ModelMapper modelMapper) {
        this.taskRequestRepository = taskRequestRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    @PreAuthorize("hasRole('TEACHER')")
    public TaskRequestDTO createRequest(TaskRequestPayload payload, Long requesterId) {
        if (payload == null || requesterId == null || requesterId <= 0) {
            throw new IllegalArgumentException("Invalid request payload or requesterId");
        }

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("Requester not found"));
        Task task = taskRepository.findById(payload.getTaskId())
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (payload.getRequestType() == null) {
            throw new IllegalArgumentException("Request type is required");
        }

        TaskRequest entity = new TaskRequest();
        entity.setTask(task);
        entity.setRequestedBy(requester);
        entity.setRequestDate(LocalDateTime.now());
        entity.setStatus(RequestStatus.PENDING);
        entity.setRemarks(payload.getRemarks());
        entity.setRequestType(payload.getRequestType());

        TaskRequest saved = taskRequestRepository.save(entity);
        return modelMapper.map(saved, TaskRequestDTO.class);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','HOD')")
    public TaskRequestDTO approveRequest(Long requestId, Long approverId) {
        TaskRequest request = getRequestById(requestId);
        User approver = getUserById(approverId);

        validateNotSelfApproval(request, approver);

        request.setStatus(RequestStatus.APPROVED);
        request.setApprovedBy(approver);

        return modelMapper.map(taskRequestRepository.save(request), TaskRequestDTO.class);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','HOD')")
    public TaskRequestDTO rejectRequest(Long requestId, Long approverId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason cannot be empty");
        }

        TaskRequest request = getRequestById(requestId);
        User approver = getUserById(approverId);

        validateNotSelfApproval(request, approver);

        request.setStatus(RequestStatus.REJECTED);
        request.setRemarks(reason);
        request.setApprovedBy(approver);

        return modelMapper.map(taskRequestRepository.save(request), TaskRequestDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskRequestDTO> getRequestsForTask(Long taskId) {
        if (taskId == null || taskId <= 0) {
            throw new IllegalArgumentException("Invalid Task ID");
        }

        return taskRequestRepository.findByTask_TaskId(taskId)
                .stream()
                .map(r -> modelMapper.map(r, TaskRequestDTO.class))
                .collect(Collectors.toList());
    }

    // ----------------- Helpers -----------------
    private TaskRequest getRequestById(Long requestId) {
        if (requestId == null || requestId <= 0) {
            throw new IllegalArgumentException("Invalid Request ID");
        }
        return taskRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
    }

    private User getUserById(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Invalid User ID");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void validateNotSelfApproval(TaskRequest request, User approver) {
        if (request.getRequestedBy().getUserId().equals(approver.getUserId())) {
            throw new RuntimeException("Users cannot approve/reject their own requests");
        }
    }
}
