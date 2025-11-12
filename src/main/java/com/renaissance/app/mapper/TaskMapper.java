package com.renaissance.app.mapper;

import com.renaissance.app.config.GlobalMapperConfig;
import com.renaissance.app.model.Task;
import com.renaissance.app.payload.TaskDTO;
import com.renaissance.app.payload.TaskPayload;
import org.mapstruct.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(
    config = GlobalMapperConfig.class,
    uses = { UserMapper.class, DepartmentMapper.class, TaskProofMapper.class, TaskRequestMapper.class },
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface TaskMapper {

    // ===================================================================
    // ENTITY → DTO
    // ===================================================================
    @Mapping(target = "createdById", source = "createdBy.userId")
    @Mapping(target = "createdByName", source = "createdBy.fullName")
    
    @Mapping(target = "startedById", source = "startedBy.userId")
    @Mapping(target = "startedByName", source = "startedBy.fullName")

    @Mapping(target = "assignedToIds", source = "assignedUsers", qualifiedByName = "mapUserIds")
    @Mapping(target = "assignedToNames", source = "assignedUsers", qualifiedByName = "mapUserNames")

    @Mapping(target = "departmentIds", source = "departments", qualifiedByName = "mapDepartmentIds")
    @Mapping(target = "departmentNames", source = "departments", qualifiedByName = "mapDepartmentNames")

    @Mapping(target = "requests", source = "requests")
    TaskDTO toDto(Task task);

    // ===================================================================
    // PAYLOAD → ENTITY
    // ===================================================================
    @Mapping(target = "taskId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "startedBy", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "approved", ignore = true)
    @Mapping(target = "proofs", ignore = true)
    @Mapping(target = "requests", ignore = true)
    @Mapping(target = "bulletins", ignore = true)
    @Mapping(target = "rfcCompletedAt", ignore = true)
    @Mapping(target = "assignedUsers", ignore = true)
    @Mapping(target = "departments", ignore = true)
    Task toEntityFromPayload(TaskPayload payload);

    // ===================================================================
    // CUSTOM MAPPINGS: Set → List<Long> / List<String>
    // ===================================================================

    @Named("mapUserIds")
    default List<Long> mapUserIds(Set<com.renaissance.app.model.User> users) {
        if (users == null || users.isEmpty()) return Collections.emptyList();
        return users.stream()
                .map(com.renaissance.app.model.User::getUserId)
                .collect(Collectors.toList());
    }

    @Named("mapUserNames")
    default List<String> mapUserNames(Set<com.renaissance.app.model.User> users) {
        if (users == null || users.isEmpty()) return Collections.emptyList();
        return users.stream()
                .map(com.renaissance.app.model.User::getFullName)
                .collect(Collectors.toList());
    }

    @Named("mapDepartmentIds")
    default List<Long> mapDepartmentIds(Set<com.renaissance.app.model.Department> departments) {
        if (departments == null || departments.isEmpty()) return Collections.emptyList();
        return departments.stream()
                .map(com.renaissance.app.model.Department::getDepartmentId)
                .collect(Collectors.toList());
    }

    @Named("mapDepartmentNames")
    default List<String> mapDepartmentNames(Set<com.renaissance.app.model.Department> departments) {
        if (departments == null || departments.isEmpty()) return Collections.emptyList();
        return departments.stream()
                .map(com.renaissance.app.model.Department::getName)
                .collect(Collectors.toList());
    }
}