package com.renaissance.app.mapper;

import com.renaissance.app.config.GlobalMapperConfig;
import com.renaissance.app.model.User;
import com.renaissance.app.payload.UserDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = GlobalMapperConfig.class)
public interface UserMapper {

    @Mapping(source = "department.departmentId", target = "departmentId")
    @Mapping(source = "department.name", target = "departmentName")
    UserDTO toDto(User user);

    @Mapping(source = "departmentId", target = "department.departmentId")
    User toEntity(UserDTO dto);
}
