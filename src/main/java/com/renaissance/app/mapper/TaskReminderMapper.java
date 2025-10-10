package com.renaissance.app.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.renaissance.app.config.GlobalMapperConfig;
import com.renaissance.app.model.TaskReminder;
import com.renaissance.app.payload.TaskReminderDTO;

@Mapper(config = GlobalMapperConfig.class, uses = {TaskMapper.class})
public interface TaskReminderMapper {

    @Mapping(source = "task.taskId", target = "taskId")
    TaskReminderDTO toDto(TaskReminder reminder);

    @Mapping(source = "taskId", target = "task.taskId")
    TaskReminder toEntity(TaskReminderDTO dto);
}
