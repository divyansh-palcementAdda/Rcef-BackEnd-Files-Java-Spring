package com.renaissance.app.payload;

import java.time.LocalDateTime;
import java.util.List;

import com.renaissance.app.model.Notification;

import lombok.Data;

@Data
public class NotificationEvent {
    private Long id;
    private Long taskId;
    private String type;
    private String message;
    private Boolean isRead;
    private LocalDateTime timestamp = LocalDateTime.now();
    private List<Long> assignedUserIds;
    private List<Long> departmentIds;
    
    public NotificationEvent() {}
    
    public NotificationEvent(Notification n) {
        this.id = n.getId();
        this.taskId = n.getTaskId();
        this.type = n.getType();
        this.message = n.getMessage();
        this.isRead = n.getIsRead();
    }
}

