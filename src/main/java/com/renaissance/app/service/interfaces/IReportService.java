package com.renaissance.app.service.interfaces;

import org.springframework.core.io.Resource;

public interface IReportService {

    Resource generateTeacherReport(Long teacherId, String format);

    Resource generateHodReport(Long hodId, String format);

    Resource generateDepartmentReport(Long departmentId, String format);

    Resource generateAllTasksReport(String format); // Added missing method
}
