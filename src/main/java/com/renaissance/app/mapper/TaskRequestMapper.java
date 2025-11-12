package com.renaissance.app.mapper;

import java.util.List;

import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.renaissance.app.config.GlobalMapperConfig;
import com.renaissance.app.model.TaskRequest;
import com.renaissance.app.payload.TaskRequestDTO;

@Mapper(config = GlobalMapperConfig.class, uses = { UserMapper.class, TaskProofMapper.class })
public interface TaskRequestMapper {

    @Mapping(source = "task.taskId", target = "taskId")
    @Mapping(source = "requestedBy.userId", target = "requestedById")
    @Mapping(source = "requestedBy.fullName", target = "requestedByName")
    @Mapping(source = "approvedBy.userId", target = "approvedById")
    @Mapping(source = "approvedBy.fullName", target = "approvedByName")
    @Mapping(source = "proofs", target = "proofs")
    TaskRequestDTO toDto(TaskRequest entity);

    List<TaskRequestDTO> toDtoList(List<TaskRequest> entities);

    @InheritInverseConfiguration
    @Mapping(target = "requestId", ignore = true)
    @Mapping(target = "requestDate", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "proofs", ignore = true)
    TaskRequest toEntity(TaskRequestDTO dto);
}