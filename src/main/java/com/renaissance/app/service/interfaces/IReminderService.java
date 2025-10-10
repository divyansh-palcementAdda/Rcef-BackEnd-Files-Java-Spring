package com.renaissance.app.service.interfaces;

import java.util.List;

import com.renaissance.app.payload.TaskReminderDTO;

public interface IReminderService {
	
	void scheduleActivationReminder(Long taskId);

	void scheduleDeadlineReminder(Long taskId);

	List<TaskReminderDTO> getRemindersForTask(Long taskId);

}
