//package com.renaissance.app.kafkas;
//
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.stereotype.Component;
//
//import com.renaissance.app.payload.TaskEventDto;
//import com.renaissance.app.service.impl.NotificationConsumer;
//
//@Component
//public class TaskEventListener {
//
//    private final NotificationConsumer notificationService;
//
//    public TaskEventListener(NotificationConsumer notificationService) {
//        this.notificationService = notificationService;
//    }
//
//    @KafkaListener(topics = "task.events", groupId = "notifications-group")
//    public void onTaskEvent(TaskEventDto event) {
//
//        notificationService.handleTaskEvent(
//                event.eventType(),
//                event.taskId(),
//                event.assignedUserIds(),
//                event.departmentIds()
//        );
//    }
//
//}
