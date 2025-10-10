package com.renaissance.app.mapper;

import org.mapstruct.Mapper;

import com.renaissance.app.config.GlobalMapperConfig;
import com.renaissance.app.model.Department;
import com.renaissance.app.payload.DepartmentDTO;

@Mapper(config = GlobalMapperConfig.class)
public interface DepartmentMapper {
    DepartmentDTO toDto(Department department);
    Department toEntity(DepartmentDTO dto);
}
