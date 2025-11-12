package com.renaissance.app.kafkas;

import java.time.LocalDate;
import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.renaissance.app.model.Task;
import com.renaissance.app.model.TaskStatus;
import com.renaissance.app.payload.NotificationEvent;
import com.renaissance.app.repository.TaskRepository;

@Component
public class NotificationScheduler {

    private final TaskRepository taskRepository;
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    public NotificationScheduler(TaskRepository taskRepository, KafkaTemplate<String, NotificationEvent> kafkaTemplate) {
        this.taskRepository = taskRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(cron = "0 0 10 * * ?")  // Every day at 10 AM
    public void sendDailyNotifications() {
        LocalDate today = LocalDate.now();
        LocalDate threeDaysAhead = today.plusDays(3);

        // 3 days before start/due
        List<Task> nearingStart = taskRepository.findByStartDate(threeDaysAhead);
        List<Task> nearingDue = taskRepository.findByDueDate(threeDaysAhead);
        sendReminders(nearingStart, "Task starting in 3 days: ");
        sendReminders(nearingDue, "Task due in 3 days: ");

        // Delayed tasks (dueDate < today and status != COMPLETED)
        List<Task> delayed = taskRepository.findByDueDateBeforeAndStatusNot(today, TaskStatus.CLOSED);
        sendReminders(delayed, "Delayed task reminder: ");
    }

    private void sendReminders(List<Task> tasks, String prefix) {
        for (Task task : tasks) {
            NotificationEvent event = new NotificationEvent();
            event.setType("REMINDER");
            event.setTaskId(task.getTaskId());
            event.setMessage(prefix + task.getTitle());
            kafkaTemplate.send("task.events", event);
        }
    }
}