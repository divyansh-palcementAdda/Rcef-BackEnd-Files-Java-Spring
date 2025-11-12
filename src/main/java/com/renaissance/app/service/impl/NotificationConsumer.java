// src/main/java/com/renaissance/app/service/impl/NotificationConsumer.java
package com.renaissance.app.service.impl;

import com.renaissance.app.model.*;
import com.renaissance.app.payload.NotificationDTO;
import com.renaissance.app.payload.NotificationEvent;
import com.renaissance.app.repository.INotificationRepository;
import com.renaissance.app.repository.IUserRepository;
import com.renaissance.app.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final INotificationRepository notificationRepository;
    private final IUserRepository userRepository;
    private final TaskRepository taskRepository;

    @KafkaListener(topics = "task.events", groupId = "notifications-group")
    @Transactional
    public void consume(NotificationEvent event) {
        Task task = taskRepository.findById(event.getTaskId())
                .orElseThrow(() -> new RuntimeException("Task not found: " + event.getTaskId()));

        // 1. Build recipient list
        Set<User> recipients = new HashSet<>(task.getAssignedUsers());

        // Add HODs of every department the task belongs to
        for (Department dept : task.getDepartments()) {
            if (dept != null) {
                userRepository.findByDepartmentsContainingAndRole(dept, Role.HOD)
                        .ifPresent(recipients::add);
            }
        }

        // 2. Persist + push for each recipient
        NotificationDTO dto = new NotificationDTO(
                event.getMessage(),
                event.getTimestamp(),
                task.getTaskId()
        );

        for (User user : recipients) {
            // Persist
            Notification n = Notification.builder()
                    .user(user)
                    .taskId(task.getTaskId())
                    .type(event.getType())
                    .message(event.getMessage())
                    .isRead(false)
                    .createdAt(event.getTimestamp())
                    .build();

            notificationRepository.save(n);

            // Push via WebSocket
            messagingTemplate.convertAndSendToUser(
                    user.getUsername(),
                    "/queue/notifications",
                    dto
            );
        }
    }
}