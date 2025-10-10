package com.renaissance.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.renaissance.app.model.TaskProof;

@Repository
public interface TaskProofRepository extends JpaRepository<TaskProof, Long> {
	List<TaskProof> findByTask_TaskId(Long taskId);

	List<TaskProof> findByUploadedBy_UserId(Long userId);
}
