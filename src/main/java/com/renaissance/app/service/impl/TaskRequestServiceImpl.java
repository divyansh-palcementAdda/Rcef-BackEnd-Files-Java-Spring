package com.renaissance.app.service.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.renaissance.app.exception.BadRequestException;
import com.renaissance.app.exception.UnauthorizedException;
import com.renaissance.app.mapper.TaskRequestMapper;
import com.renaissance.app.model.RequestStatus;
import com.renaissance.app.model.RequestType;
import com.renaissance.app.model.Task;
import com.renaissance.app.model.TaskProof;
import com.renaissance.app.model.TaskRequest;
import com.renaissance.app.model.TaskStatus;
import com.renaissance.app.model.User;
import com.renaissance.app.payload.ApproveRequestPayload;
import com.renaissance.app.payload.TaskRequestDTO;
import com.renaissance.app.payload.TaskRequestMultipartPayload;
import com.renaissance.app.repository.IUserRepository;
import com.renaissance.app.repository.TaskRepository;
import com.renaissance.app.repository.TaskRequestRepository;
import com.renaissance.app.service.interfaces.ITaskRequestService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class TaskRequestServiceImpl implements ITaskRequestService {

    private final TaskRequestRepository taskRequestRepository;
    private final TaskRepository taskRepository;
    private final IUserRepository userRepository;
    private final TaskRequestMapper taskRequestMapper;
    private final TaskProofService taskProofService;

    // --------------------------------------------------------------
    // SINGLE API: create request + upload proofs
    // --------------------------------------------------------------
    @Override
    @PreAuthorize("hasAnyRole('ADMIN','HOD','TEACHER')")
    public TaskRequestDTO createRequestWithProofs(
            Long taskId,
            TaskRequestMultipartPayload payload,
            Long requesterId) throws UnauthorizedException, IOException, BadRequestException {

        // ---- 1. Validate ------------------------------------------------
        validatePayload(payload, requesterId, taskId);
        User requester = getUserById(requesterId);
        Task task = getTaskById(taskId);
        validate(task, requester, payload);

        // ---- 2. Save request --------------------------------------------
        TaskRequest request = TaskRequest.builder()
                .task(task)
                .requestedBy(requester)
                .requestType(payload.requestType())
                .remarks(payload.remarks())
                .requestDate(LocalDateTime.now())
                .status(RequestStatus.PENDING)
                .build();

        TaskRequest savedRequest = taskRequestRepository.save(request);

        // ---- 3. Upload proofs (if any) ----------------------------------
        List<TaskProof> proofs = new ArrayList<>();
        if (payload.proofs() != null && !payload.proofs().isEmpty()) {
            for (MultipartFile file : payload.proofs()) {
                TaskProof proof = taskProofService.uploadProof(
                        file,
                        taskId,
                        savedRequest.getRequestId(),
                        requester);
                proofs.add(proof);
            }
        }

        // ---- 4. Refresh request with proofs (JPA will load them) --------
        TaskRequest refreshed = taskRequestRepository.findById(savedRequest.getRequestId())
                .orElseThrow(() -> new RuntimeException("Request disappeared"));

        return taskRequestMapper.toDto(refreshed);   // includes proofs
    }

    // --------------------------------------------------------------
    // Validation
    // --------------------------------------------------------------
    private void validatePayload(TaskRequestMultipartPayload p, Long requesterId, Long taskId) {
        if (p == null || requesterId == null || taskId == null) {
            throw new IllegalArgumentException("Invalid payload");
        }
        if (p.requestType() == null) {
            throw new IllegalArgumentException("Request type required");
        }
        if (p.requestType() == RequestType.EXTENSION && (p.remarks() == null || p.remarks().isBlank())) {
            throw new IllegalArgumentException("Reason required for EXTENSION");
        }
        if (p.requestType() == RequestType.CLOSURE && (p.proofs() == null || p.proofs().isEmpty())) {
            throw new IllegalArgumentException("At least one proof required for CLOSURE");
        }
    }

    private void validate(Task task, User requester, TaskRequestMultipartPayload p) throws UnauthorizedException {
        if (!task.getAssignedUserIds().contains(requester.getUserId())) {
            throw new UnauthorizedException("Not assigned to task");
        }

        task.setStatus(switch (p.requestType()) {
            case CLOSURE -> TaskStatus.REQUEST_FOR_CLOSURE;
            case EXTENSION -> TaskStatus.REQUEST_FOR_EXTENSION;
        });
        task.setUpdatedAt(LocalDateTime.now());
        if (p.requestType() == RequestType.CLOSURE) {
            task.setRfcCompletedAt(LocalDateTime.now());
        }
        taskRepository.save(task);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public TaskRequestDTO approveRequest(ApproveRequestPayload payload, Long approverId) {
        TaskRequest request = getRequestById(payload.getRequestId());
        User approver = getUserById(approverId);
        request.setStatus(RequestStatus.APPROVED);
        request.setApprovedBy(approver);

        Task task = request.getTask();
        updateTaskStatusOnApproval(request, task, payload);
        System.err.println("*********************************1");

        taskRequestRepository.save(request);
        System.err.println("*********************************2");
        taskRepository.save(task);
        System.err.println("*********************************3");

        return taskRequestMapper.toDto(request);
    }
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public TaskRequestDTO rejectRequest(Long requestId, Long approverId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }

        TaskRequest request = getRequestById(requestId);
        User approver = getUserById(approverId);

        request.setStatus(RequestStatus.REJECTED);
        request.setApprovedBy(approver);
        request.setRemarks(reason);

        Task task = request.getTask();
        updateTaskStatusOnRejection(request, task); // Same logic as approval, but for rejection

        taskRequestRepository.save(request);
        taskRepository.save(task); // Save task changes

        return taskRequestMapper.toDto(request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskRequestDTO> getRequestsForTask(Long taskId) {
        if (taskId == null || taskId <= 0) {
            throw new IllegalArgumentException("Invalid Task ID");
        }
        return taskRequestRepository.findByTask_TaskId(taskId)
                .stream()
                .map(taskRequestMapper::toDto)
                .toList();
    }

    // === Helpers ===
    
    
    /** Handles CLOSURE vs EXTENSION logic */
    private void updateTaskStatusOnApproval(TaskRequest request, Task task, ApproveRequestPayload payload) {
        if (request.getRequestType() == RequestType.CLOSURE) {
            // Closure → mark task CLOSED, set closure date = today
            task.setStatus(TaskStatus.CLOSED);
            task.setRfcCompletedAt(LocalDateTime.now());

        } else if (request.getRequestType() == RequestType.EXTENSION) {
            // Extension → update due date if provided
            if (payload.getNewDueDate() == null) {
                throw new IllegalArgumentException("newDueDate is required for EXTENSION approval");
            }
            LocalDate newDue = payload.getNewDueDate().toLocalDate();      
            task.setDueDate(newDue.atStartOfDay());
            task.setStatus(TaskStatus.EXTENDED);
        }
    }
    private void updateTaskStatusOnRejection(TaskRequest request, Task task) {
        task.setUpdatedAt(LocalDateTime.now());

        switch (request.getRequestType()) {
            case CLOSURE:
                // Revert from REQUEST_FOR_CLOSURE to IN_PROGRESS or previous state
                task.setStatus(TaskStatus.IN_PROGRESS);
                task.setRfcCompletedAt(null); // Clear RFC timestamp
                break;

            case EXTENSION:
                task.setStatus(TaskStatus.IN_PROGRESS); // or keep previous
                break;
            default:
                break;
        }
    }
    

//    private void validate(Task task, User requester, TaskRequestDTO payload) throws UnauthorizedException {
//        if (task == null || requester == null || payload == null) {
//            throw new IllegalArgumentException("Task, requester, and payload must not be null");
//        }
//
//        Long requesterId = requester.getUserId();
//        if (requesterId == null) {
//            throw new IllegalArgumentException("Requester must have a valid user ID");
//        }
//
//        // Check if the requester is assigned to the task
//        if (!task.getAssignedUserIds().contains(requesterId)) {
//            throw new UnauthorizedException("User is not assigned to this task");
//        }
//
//        RequestType requestType = payload.getRequestType();
//        if (requestType == null) {
//            throw new IllegalArgumentException("Request type cannot be null");
//        }
//
//        // Update task status based on request type
//        switch (requestType) {
//            case CLOSURE:
//                task.setStatus(TaskStatus.REQUEST_FOR_CLOSURE);
//                task.setRfcCompletedAt(LocalDateTime.now());
//                task.setUpdatedAt(LocalDateTime.now());
//                break;
//
//            case EXTENSION:
//                task.setStatus(TaskStatus.REQUEST_FOR_EXTENSION);
//                task.setUpdatedAt(LocalDateTime.now());
//                break;
//            default:
//                throw new IllegalArgumentException("Unsupported request type: " + requestType);
//        }
//
//        taskRepository.save(task);
//        return;
//    }

    
//    private void validatePayload(TaskRequestDTO payload, Long requesterId) {
//        if (payload == null || requesterId == null || requesterId <= 0) {
//            throw new IllegalArgumentException("Invalid payload or requester");
//        }
//        if (payload.getTaskId() == null || payload.getRequestType() == null) {
//            throw new IllegalArgumentException("Task ID and Request Type are required");
//        }
//    }

    private TaskRequest getRequestById(Long id) {
        return taskRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found: " + id));
    }

    private User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    private Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found: " + id));
    }

}