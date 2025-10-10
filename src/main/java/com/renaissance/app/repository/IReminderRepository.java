package com.renaissance.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.renaissance.app.model.TaskReminder;

public interface IReminderRepository extends JpaRepository<TaskReminder, Long> {

	List<TaskReminder> findByTask_TaskId(Long taskId);
}
