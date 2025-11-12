// src/main/java/com/renaissance/app/payload/NotificationResponseDTO.java
package com.renaissance.app.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDTO {
    private Long id;
    private Long taskId;
    private String type;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;

    // Only what frontend needs
    private String username;  // instead of full User
    private String useristitle; // optional: "Task Approved", etc.
}