package com.renaissance.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.renaissance.app.model.BulkUploadLog;

public interface IBulkUploadLogRepository extends JpaRepository<BulkUploadLog, Long> {

}
