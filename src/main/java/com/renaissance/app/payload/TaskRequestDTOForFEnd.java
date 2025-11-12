package com.renaissance.app.payload;

import java.time.LocalDateTime;
import java.util.List;

import com.renaissance.app.model.RequestStatus;
import com.renaissance.app.model.RequestType;

//DTO returned to frontend
public record TaskRequestDTOForFEnd(
     Long requestId,
     Long taskId,
     RequestType requestType,
     String remarks,
     RequestStatus status,
     Long requestedBy,
     LocalDateTime requestDate,
     List<TaskProofDTO> proofs           // <-- now included
) {}
