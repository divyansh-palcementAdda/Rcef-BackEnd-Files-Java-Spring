package com.renaissance.app.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.renaissance.app.config.GlobalMapperConfig;
import com.renaissance.app.model.TaskProof;
import com.renaissance.app.payload.TaskProofDTO;

@Mapper(config = GlobalMapperConfig.class, uses = { UserMapper.class })
public interface TaskProofMapper {

    @Mapping(source = "uploadedBy.userId", target = "uploadedById")
    @Mapping(source = "uploadedBy.fullName", target = "uploadedByName")
    TaskProofDTO toDto(TaskProof proof);

    List<TaskProofDTO> toDtoList(List<TaskProof> proofs);
}