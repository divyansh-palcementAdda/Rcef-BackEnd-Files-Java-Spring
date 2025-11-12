package com.renaissance.app.payload;


import java.time.LocalDateTime;
import java.util.List;

public record TaskEventDto(
        String eventType,
        Long taskId,
        List<Long> assignedUserIds,
        List<Long> departmentIds,
        LocalDateTime timestamp
) {}

