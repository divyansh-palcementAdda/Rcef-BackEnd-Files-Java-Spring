package com.renaissance.app.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.renaissance.app.exception.ResourcesNotFoundException;
import com.renaissance.app.exception.UnauthorizedException;
import com.renaissance.app.model.RequestType;
import com.renaissance.app.payload.ApiResult;
import com.renaissance.app.payload.ApproveRequestPayload;
import com.renaissance.app.payload.RejectRequestPayload;
import com.renaissance.app.payload.TaskRequestDTO;
import com.renaissance.app.payload.TaskRequestMultipartPayload;
import com.renaissance.app.service.impl.JwtService;
import com.renaissance.app.service.interfaces.ITaskRequestService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tasks/{taskId}/requests")
@RequiredArgsConstructor  // Lombok handles constructor injection
public class TaskRequestController {

    private final ITaskRequestService taskRequestService;
    private final JwtService jwtService;  // Add this to extract userId from JWT

    // -------------------------------
    // CREATE REQUEST + PROOFS (Single API)
    // -------------------------------
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HOD','TEACHER')")
    public ResponseEntity<ApiResult<TaskRequestDTO>> createRequestWithProofs(
            @PathVariable Long taskId,
            @RequestParam("requestType") RequestType requestType,
            @RequestParam(value = "remarks", required = false) String remarks,
            @RequestParam(value = "proofs", required = false) List<MultipartFile> proofs,
            Authentication authentication)  {

        // --- Extract userId from JWT ---
        Long requesterId = null;
		try {
			requesterId = jwtService.getUserIdFromAuthentication();
		} catch (ResourcesNotFoundException e) {
			 return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                    .body(ApiResult.error("Invalid or missing token", HttpStatus.UNAUTHORIZED));
		}
        if (requesterId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResult.error("Invalid or missing token", HttpStatus.UNAUTHORIZED));
        }

        // --- Build payload ---
        TaskRequestMultipartPayload payload = new TaskRequestMultipartPayload(
                requestType,
                remarks,
                proofs != null ? proofs : List.of()
        );

        try {
            TaskRequestDTO dto = taskRequestService.createRequestWithProofs(taskId, payload, requesterId);
            return ResponseEntity.ok(ApiResult.ok(dto, "Request created with proofs"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResult.error(e.getMessage(), HttpStatus.BAD_REQUEST));
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResult.error(e.getMessage(), HttpStatus.FORBIDDEN));
        } catch (Exception e) {
            // Catch unexpected errors (e.g. file upload, DB)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResult.error("Failed to process request: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }


    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HOD','TEACHER')")
    public ResponseEntity<ApiResult<List<TaskRequestDTO>>> getRequests(@PathVariable Long taskId) {
        try {
            List<TaskRequestDTO> requests = taskRequestService.getRequestsForTask(taskId);
            return ResponseEntity.ok(ApiResult.ok(requests));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResult.error(e.getMessage(), HttpStatus.NOT_FOUND));
        }
    }

    @PatchMapping("/{requestId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<TaskRequestDTO>> approveRequest(
            @PathVariable Long taskId,
            @PathVariable Long requestId,
            @RequestBody @Valid ApproveRequestPayload payload,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Validate requestId matches
        if (!payload.getRequestId().equals(requestId)) {
            return ResponseEntity.badRequest()
                    .body(ApiResult.error("Request ID mismatch", HttpStatus.BAD_REQUEST));
        }

        Long approverId = null;
		try {
			approverId = jwtService.getUserIdFromAuthentication();
		} catch (ResourcesNotFoundException e) {
			e.printStackTrace();
		}
        if (approverId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResult.error("Invalid token", HttpStatus.UNAUTHORIZED));
        }

        try {
            TaskRequestDTO dto = taskRequestService.approveRequest(payload, approverId);
            return ResponseEntity.ok(ApiResult.ok(dto, "Request approved successfully"));
        } catch (IllegalArgumentException e) {
        	System.err.println(e);
			e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(ApiResult.error(e.getMessage(), HttpStatus.BAD_REQUEST));
        } catch (Exception e) {
        	System.err.println(e);
			e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResult.error("Approval failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }
    @PatchMapping("/{requestId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<TaskRequestDTO>> rejectRequest(
            @PathVariable Long taskId,
            @PathVariable Long requestId,
            @RequestBody @Valid RejectRequestPayload payload,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (!payload.getRequestId().equals(requestId)) {
            return ResponseEntity.badRequest()
                    .body(ApiResult.error("Request ID mismatch", HttpStatus.BAD_REQUEST));
        }

        Long approverId = null;
		try {
			approverId = jwtService.getUserIdFromAuthentication();
		} catch (ResourcesNotFoundException e) {
			e.printStackTrace();
		}
        if (approverId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResult.error("Invalid token", HttpStatus.UNAUTHORIZED));
        }

        try {
            TaskRequestDTO dto = taskRequestService.rejectRequest(requestId, approverId, payload.getReason());
            return ResponseEntity.ok(ApiResult.ok(dto, "Request rejected"));
        } catch (IllegalArgumentException e) {
        	System.err.println(e);
            return ResponseEntity.badRequest()
                    .body(ApiResult.error(e.getMessage(), HttpStatus.BAD_REQUEST));
        } catch (Exception e) {
        	System.err.println(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResult.error("Rejection failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }
}