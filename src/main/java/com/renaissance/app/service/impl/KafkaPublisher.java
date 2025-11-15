// src/main/java/com/renaissance/app/service/impl/KafkaPublisher.java
package com.renaissance.app.service.impl;

import java.time.LocalDateTime;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.renaissance.app.model.Task;
import com.renaissance.app.payload.NotificationEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KafkaPublisher {

    // Use the SAME type as in KafkaConfig: NotificationEvent
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    // KafkaPublisher.java
    public void publishTaskEvent(String eventType, Task task, String message) {
        NotificationEvent event = new NotificationEvent();
        event.setType(eventType);
        event.setTaskId(task.getTaskId());
        event.setMessage(message);
        event.setAssignedUserIds(task.getAssignedUserIds());
        event.setDepartmentIds(task.getDepartmentIds());
        event.setTimestamp(LocalDateTime.now());

        // Use taskId as key → same partition
        kafkaTemplate.send("task.events", task.getTaskId().toString(), event);
    }

    public void publishTaskEvent(String key, NotificationEvent event) {
        kafkaTemplate.send("task.events", key, event);
    }
}