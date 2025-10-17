package com.renaissance.app.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.renaissance.app.config.GlobalMapperConfig;
import com.renaissance.app.model.Task;
import com.renaissance.app.payload.TaskDTO;
import com.renaissance.app.payload.TaskPayload;

@Mapper(
    config = GlobalMapperConfig.class,
    uses = { UserMapper.class, DepartmentMapper.class },
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface TaskMapper {

    // ------------------ ENTITY → DTO ------------------
    @Mapping(target = "createdById", source = "createdBy.userId")
    @Mapping(target = "createdByName", source = "createdBy.fullName")

    @Mapping(target = "assignedToIds", expression = "java(task.getAssignedUsers() != null ? task.getAssignedUsers().stream().map(User::getUserId).toList() : java.util.Collections.emptyList())")
    @Mapping(target = "assignedToNames", expression = "java(task.getAssignedUsers() != null ? task.getAssignedUsers().stream().map(User::getFullName).toList() : java.util.Collections.emptyList())")

    @Mapping(target = "departmentIds", expression = "java(task.getDepartments() != null ? task.getDepartments().stream().map(Department::getDepartmentId).toList() : java.util.Collections.emptyList())")
    @Mapping(target = "departmentNames", expression = "java(task.getDepartments() != null ? task.getDepartments().stream().map(Department::getName).toList() : java.util.Collections.emptyList())")
    TaskDTO toDto(Task task);

    // ------------------ PAYLOAD → ENTITY ------------------
    @Mapping(target = "taskId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "assignedUsers", ignore = true)
    @Mapping(target = "departments", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "approved", ignore = true)
    @Mapping(target = "proofs", ignore = true)
    @Mapping(target = "requests", ignore = true)
    @Mapping(target = "bulletins", ignore = true)
    @Mapping(target = "rfcCompletedAt", ignore = true)
    Task toEntityFromPayload(TaskPayload payload);
}
