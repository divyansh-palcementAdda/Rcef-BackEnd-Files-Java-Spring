package com.renaissance.app.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import com.renaissance.app.config.GlobalMapperConfig;
import com.renaissance.app.model.Task;
import com.renaissance.app.payload.TaskDTO;
import com.renaissance.app.payload.TaskPayload;

@Mapper(config = GlobalMapperConfig.class, uses = { UserMapper.class, DepartmentMapper.class })
public interface TaskMapper {

    // ------------------ ENTITY → DTO ------------------
    @Mappings({
        @Mapping(source = "createdBy.userId", target = "createdById"),
        @Mapping(source = "createdBy.fullName", target = "createdByName"),
        @Mapping(source = "assignedTo.userId", target = "assignedToId"),
        @Mapping(source = "assignedTo.fullName", target = "assignedToName"),
        @Mapping(source = "department.departmentId", target = "departmentId"),
        @Mapping(source = "department.name", target = "departmentName")
    })
    TaskDTO toDto(Task task);

    // ------------------ DTO → ENTITY ------------------
    @Mappings({
        @Mapping(source = "createdById", target = "createdBy.userId"),
        @Mapping(source = "assignedToId", target = "assignedTo.userId"),
        @Mapping(source = "departmentId", target = "department.departmentId")
    })
    Task toEntity(TaskDTO dto);

    // ------------------ PAYLOAD → DTO ------------------
    /**
     * Converts TaskPayload (request body) → TaskDTO
     * Used for mapping incoming create/update payloads before saving.
     */
    @Mappings({
        @Mapping(target = "taskId", ignore = true),
        @Mapping(target = "createdById", ignore = true),
        @Mapping(target = "createdByName", ignore = true),
        @Mapping(target = "assignedToName", ignore = true),
        @Mapping(target = "departmentName", ignore = true),
        @Mapping(target = "proofs", ignore = true),
        @Mapping(target = "requests", ignore = true),
        @Mapping(target = "approved", ignore = true),
        @Mapping(target = "rfcCompletedAt", ignore = true)
    })
    TaskDTO toDtoFromPayload(TaskPayload payload);

    // ------------------ PAYLOAD → ENTITY ------------------
    /**
     * Converts TaskPayload → Task entity.
     * Used for new task creation or updating from payload directly.
     */
    @Mappings({
        @Mapping(target = "taskId", ignore = true),
        @Mapping(target = "createdBy", ignore = true),
        @Mapping(target = "assignedTo", ignore = true),
        @Mapping(target = "department", ignore = true),
        @Mapping(target = "createdAt", ignore = true),
        @Mapping(target = "updatedAt", ignore = true),
        @Mapping(target = "approved", ignore = true),
        @Mapping(target = "proofs", ignore = true),
        @Mapping(target = "requests", ignore = true),
        @Mapping(target = "rfcCompletedAt", ignore = true)
    })
    Task toEntityFromPayload(TaskPayload payload);
}
