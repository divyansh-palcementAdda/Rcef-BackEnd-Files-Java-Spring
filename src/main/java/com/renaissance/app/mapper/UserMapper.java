package com.renaissance.app.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.renaissance.app.config.GlobalMapperConfig;
import com.renaissance.app.model.Department;
import com.renaissance.app.model.User;
import com.renaissance.app.payload.UserDTO;

@Mapper(config = GlobalMapperConfig.class)
public interface UserMapper {

    @Mapping(source = "departments", target = "departmentIds", qualifiedByName = "mapDepartmentsToIds")
    @Mapping(source = "departments", target = "departmentNames", qualifiedByName = "mapDepartmentsToNames")
    UserDTO toDto(User user);

    @Mapping(source = "departmentIds", target = "departments", qualifiedByName = "mapIdsToDepartments")
    User toEntity(UserDTO dto);

    @Named("mapDepartmentsToIds")
    default List<Long> mapDepartmentsToIds(List<Department> departments) {
        if (departments == null) return null;
        return departments.stream()
                .map(Department::getDepartmentId)
                .collect(Collectors.toList());
    }

    @Named("mapDepartmentsToNames")
    default List<String> mapDepartmentsToNames(List<Department> departments) {
        if (departments == null) return null;
        return departments.stream()
                .map(Department::getName)
                .collect(Collectors.toList());
    }

    @Named("mapIdsToDepartments")
    default List<Department> mapIdsToDepartments(List<Long> ids) {
        if (ids == null) return null;
        return ids.stream()
                .map(id -> Department.builder().departmentId(id).build())
                .collect(Collectors.toList());
    }
}
