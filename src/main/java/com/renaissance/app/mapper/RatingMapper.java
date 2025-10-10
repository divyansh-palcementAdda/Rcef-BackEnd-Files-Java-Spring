package com.renaissance.app.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.renaissance.app.config.GlobalMapperConfig;
import com.renaissance.app.model.Rating;
import com.renaissance.app.payload.RatingDTO;

@Mapper(config = GlobalMapperConfig.class, uses = {UserMapper.class, DepartmentMapper.class, TaskMapper.class})
public interface RatingMapper {

    @Mapping(source = "ratedUser.userId", target = "ratedUserId")
    @Mapping(source = "ratedUser.fullName", target = "ratedUserName")
    @Mapping(source = "ratedDepartment.departmentId", target = "ratedDepartmentId")
    @Mapping(source = "ratedDepartment.name", target = "ratedDepartmentName")
    @Mapping(source = "task.taskId", target = "taskId")
    @Mapping(source = "task.title", target = "taskTitle")
    @Mapping(source = "givenBy.userId", target = "givenById")
    @Mapping(source = "givenBy.fullName", target = "givenByName")
    RatingDTO toDto(Rating rating);

    @Mapping(source = "ratedUserId", target = "ratedUser.userId")
    @Mapping(source = "ratedDepartmentId", target = "ratedDepartment.departmentId")
    @Mapping(source = "taskId", target = "task.taskId")
    @Mapping(source = "givenById", target = "givenBy.userId")
    Rating toEntity(RatingDTO dto);
}
