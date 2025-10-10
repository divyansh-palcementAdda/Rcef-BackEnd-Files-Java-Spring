package com.renaissance.app.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.renaissance.app.config.GlobalMapperConfig;
import com.renaissance.app.model.AuditLog;
import com.renaissance.app.payload.AuditLogDTO;

@Mapper(config = GlobalMapperConfig.class, uses = {UserMapper.class})
public interface AuditLogMapper {

    @Mapping(source = "user.userId", target = "userId")
    @Mapping(source = "user.fullName", target = "username")
    AuditLogDTO toDto(AuditLog log);

    @Mapping(source = "userId", target = "user.userId")
    AuditLog toEntity(AuditLogDTO dto);
}
