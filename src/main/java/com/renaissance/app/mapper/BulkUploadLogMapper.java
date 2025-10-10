package com.renaissance.app.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.renaissance.app.config.GlobalMapperConfig;
import com.renaissance.app.model.BulkUploadLog;
import com.renaissance.app.payload.BulkUploadLogDTO;

@Mapper(config = GlobalMapperConfig.class, uses = {UserMapper.class})
public interface BulkUploadLogMapper {

    @Mapping(source = "uploadedBy.userId", target = "uploadedById")
    @Mapping(source = "uploadedBy.fullName", target = "uploadedByName")
    BulkUploadLogDTO toDto(BulkUploadLog log);

    @Mapping(source = "uploadedById", target = "uploadedBy.userId")
    BulkUploadLog toEntity(BulkUploadLogDTO dto);
}
