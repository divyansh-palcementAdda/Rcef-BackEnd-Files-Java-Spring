package com.renaissance.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.renaissance.app.model.RequestStatus;
import com.renaissance.app.model.RequestType;
import com.renaissance.app.model.TaskRequest;

@Repository
public interface TaskRequestRepository extends JpaRepository<TaskRequest, Long> {
	List<TaskRequest> findByTask_TaskId(Long taskId);

	List<TaskRequest> findByRequestedBy_UserId(Long userId);

	List<TaskRequest> findByRequestType(RequestType type);

	List<TaskRequest> findByStatus(RequestStatus status);
}
