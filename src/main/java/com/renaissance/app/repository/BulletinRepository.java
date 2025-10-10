package com.renaissance.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.renaissance.app.model.Bulletin;
import com.renaissance.app.model.Severity;

@Repository
public interface BulletinRepository extends JpaRepository<Bulletin, Long> {

	List<Bulletin> findByTask_TaskId(Long taskId);

	List<Bulletin> findBySeverity(Severity severity);
}
